# Multi-DB

## 1. 개요

하나의 애플리케이션을 여러 DBMS(Oracle, MySQL, PostgreSQL 등)에 배포할 수 있도록, DB별 차이를 흡수하는 전략을 정의한다. SQL 작성 표준은 SQL 문서 참고.

**핵심 원칙:**

- DB 전환 시 **Java 코드 변경은 0건**이어야 한다
- DB별 차이는 **설정 파일과 Mapper XML에서만** 흡수한다
- 페이징은 **MyBatis XML에서 직접 처리**한다

---

## 2. 프로파일 기반 DB 전환

> 프로파일 그룹 설계는 Configuration 3절 참고.

### 2.1 구조

Spring Profile로 DB를 전환한다. 동시 사용이 아닌 **배포 환경에 따라 하나의 DB를 선택**하는 구조이다.

```
application.yml
  └─ spring.profiles.active: oracle
       ├─ oracle → application-oracle.yml
       └─ mysql  → application-mysql.yml
```

### 2.2 공통 설정

DB에 무관한 MyBatis 공통 설정은 SQL 2.1절 참고.

### 2.3 DB별 설정 (application-{db}.yml)

```yaml
spring:
  datasource:
    driver-class-name: (DB별 드라이버)
    url: (접속 URL)
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      pool-name: {db}-pool
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 3. Mapper XML DB별 분리 전략

### 3.1 주 전략: 디렉터리 기반 분리

DB별로 **별도 디렉터리**에 Mapper XML을 관리하고, 프로파일별 `mybatis.mapper-locations`로 해당 DB의 XML만 로딩한다.

```
resources/mapper/
├── oracle/
│   ├── log/AccessLogMapper.xml
│   └── security/AuthorityMapper.xml
└── mysql/
    ├── log/AccessLogMapper.xml
    └── security/AuthorityMapper.xml
```

```yaml
# application-oracle.yml
mybatis:
  mapper-locations: classpath:mapper/oracle/**/*.xml

# application-mysql.yml
mybatis:
  mapper-locations: classpath:mapper/mysql/**/*.xml
```

> **장점:** DB별 SQL이 물리적으로 분리되어 있어 혼동이 없고, 각 XML 파일에 `databaseId` 속성을 붙일 필요가 없다.

### 3.2 보조 전략: DatabaseIdProvider

`MyBatisConfig.java`에 `DatabaseIdProvider` Bean이 등록되어 있다. 디렉터리 분리가 주 전략이지만, 하나의 XML 안에서 개별 SQL만 DB별로 분기해야 할 경우 `databaseId` 속성을 보조적으로 사용할 수 있다.

```java
@Configuration
public class MyBatisConfig {

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("Oracle", "oracle");
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("PostgreSQL", "postgresql");
        provider.setProperties(properties);
        return provider;
    }
}
```

- `databaseId` 없는 SQL → 모든 DB에서 사용 (공통)
- `databaseId="oracle"` → Oracle에서만 사용
- `databaseId="mysql"` → MySQL에서만 사용

### 3.3 DB별 SQL 작성 예시

디렉터리가 분리되어 있으므로, 각 DB 디렉터리의 XML에 해당 DB 문법으로 직접 작성한다.

**Batch Insert:**

```xml
<!-- mapper/oracle/.../Mapper.xml -->
<insert id="insertBatch">
    INSERT INTO ROLE_MENUS (ROLE_ID, MENU_ID, AUTH_CODE)
    SELECT A.* FROM (
    <foreach collection="list" item="item" separator=" UNION ALL ">
        SELECT #{item.roleId}, #{item.menuId}, #{item.authCode} FROM DUAL
    </foreach>
    ) A
</insert>

<!-- mapper/mysql/.../Mapper.xml -->
<insert id="insertBatch">
    INSERT INTO ROLE_MENUS (ROLE_ID, MENU_ID, AUTH_CODE)
    VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.roleId}, #{item.menuId}, #{item.authCode})
    </foreach>
</insert>
```

> Batch Insert 전체 코드는 SQL 7.3절 참고.

**NULL 대체 함수:**

```xml
<!-- mapper/oracle/.../Mapper.xml -->
<select id="getNextSeq" resultType="int">
    SELECT NVL(MAX(seq), 0) + 1 FROM history WHERE app_id = #{appId}
</select>

<!-- mapper/mysql/.../Mapper.xml -->
<select id="getNextSeq" resultType="int">
    SELECT IFNULL(MAX(seq), 0) + 1 FROM history WHERE app_id = #{appId}
</select>
```

**LIKE 검색 (문자열 결합):**

```xml
<!-- mapper/oracle/.../Mapper.xml -->
<select id="search" resultType="UserDTO">
    SELECT id, name FROM users
    WHERE name LIKE '%' || #{keyword} || '%'
</select>

<!-- mapper/mysql/.../Mapper.xml -->
<select id="search" resultType="UserDTO">
    SELECT id, name FROM users
    WHERE name LIKE CONCAT('%', #{keyword}, '%')
</select>
```

---

## 4. DB별 SQL 문법 차이 요약

| 기능 | Oracle | MySQL | 공통화 방법 |
| --- | --- | --- | --- |
| 문자열 결합 | `\|\|` | `CONCAT()` | databaseId 분기 |
| NULL 대체 | `NVL()` | `IFNULL()` | databaseId 분기 |
| Batch Insert | `DUAL + UNION ALL` | `VALUES (...), (...)` | databaseId 분기 |
| 페이징 | `ROWNUM` | `LIMIT` | databaseId 분기 |
| 현재 시간 | `SYSDATE` | `NOW()` | databaseId 분기 |
| 시퀀스 | `SEQ.NEXTVAL` | `AUTO_INCREMENT` | databaseId 분기 |
| 타입 변환 | `TO_CHAR()` / `TO_NUMBER()` | `CAST()` | databaseId 분기 |
| 빈 SELECT | `FROM DUAL` 필요 | 불필요 | databaseId 분기 |
| 스키마 지정 | `ALTER SESSION SET CURRENT_SCHEMA` | DB명을 URL에 포함 | application.yml |

---

## 6. 체크리스트

DB 전환 시 확인 항목:

- [ ]  `spring.profiles.active` 변경
- [ ]  `application-{db}.yml` 설정 완료 (DataSource, HikariCP, mapper-locations, pagehelper dialect)
- [ ]  `mapper/{db}/` 디렉터리에 해당 DB용 Mapper XML 작성 완료
- [ ]  DatabaseIdProvider Bean 등록 확인 (보조 전략 사용 시)
- [ ]  DDL 스크립트로 테이블/초기 데이터 생성 완료
- [ ]  기본 CRUD + 주요 조회 화면 동작 검증 완료

---

*Last updated: 2026-02-26*
