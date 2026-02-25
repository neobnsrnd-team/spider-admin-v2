# Multi-DB

## 1. 개요

하나의 애플리케이션을 여러 DBMS(Oracle, MySQL, PostgreSQL 등)에 배포할 수 있도록, DB별 차이를 흡수하는 전략을 정의한다. SQL 작성 표준은 SQL 문서 참고.

**핵심 원칙:**

- DB 전환 시 **Java 코드 변경은 0건**이어야 한다
- DB별 차이는 **설정 파일과 Mapper XML에서만** 흡수한다
- 페이징 등 공통 처리는 **프레임워크(PageHelper)에 위임**한다

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

> PageHelper 설정은 SQL 2.2절 참고. DB별 `helper-dialect`만 변경한다.

---

## 3. DatabaseIdProvider를 이용한 SQL 공통화

### 3.1 개념

MyBatis 내장 기능으로, **하나의 XML 파일 안에서 DB별 SQL을 분기**할 수 있다.

- `databaseId` 없는 SQL → 모든 DB에서 사용 (공통)
- `databaseId="oracle"` → Oracle에서만 사용
- `databaseId="mysql"` → MySQL에서만 사용
- **우선순위:** databaseId 일치 > databaseId 없음

### 3.2 설정

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

### 3.3 SQL 작성 규칙

SQL은 표준 SQL과 DB별 분기의 두 가지로 구분한다.

### 3.4 표준 SQL 규칙

표준 SQL은 `databaseId`를 붙이지 않는다.

```xml
<!-- 공통: SELECT, UPDATE, DELETE 등 표준 SQL -->
<select id="selectById" resultMap="BaseResultMap">
    SELECT id, name, status FROM users WHERE id = #{id}
</select>

<update id="update">
UPDATE users SET name = #{name} WHERE id = #{id}
</update>

<delete id="deleteById">
DELETE FROM users WHERE id = #{id}
</delete>
```

### 3.5 DB별 분기 규칙

같은 SQL ID로 DB별 버전을 각각 작성한다. MyBatis가 현재 DB에 맞는 것을 자동 선택한다.

**Batch Insert:**

```xml
<insert id="insertBatch" databaseId="oracle">...</insert>
<insert id="insertBatch" databaseId="mysql">...</insert>
```

> Batch Insert 전체 코드는 SQL 7.3절 참고.

**NULL 대체 함수:**

```xml
<!-- Oracle -->
<select id="getNextSeq" databaseId="oracle" resultType="int">
    SELECT NVL(MAX(seq), 0) + 1 FROM history WHERE app_id = #{appId}
</select>

        <!-- MySQL -->
<select id="getNextSeq" databaseId="mysql" resultType="int">
SELECT IFNULL(MAX(seq), 0) + 1 FROM history WHERE app_id = #{appId}
</select>
```

**LIKE 검색 (문자열 결합):**

```xml
<!-- Oracle -->
<select id="search" databaseId="oracle" resultType="UserDTO">
    SELECT id, name FROM users
    WHERE name LIKE '%' || #{keyword} || '%'
</select>

        <!-- MySQL -->
<select id="search" databaseId="mysql" resultType="UserDTO">
SELECT id, name FROM users
WHERE name LIKE CONCAT('%', #{keyword}, '%')
</select>
```

### 3.6 Java 코드 DB 무관성 원칙

```java
// Mapper 인터페이스 - DB에 무관하게 동일
@Mapper
public interface UserMapper {
    User selectById(String id);
    int insert(User user);
    int insertBatch(List<User> list);   // XML에서 DB별 분기
}

// Service - DB에 무관하게 동일
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
}
```

---

## 4. 페이징 처리

SQL 9절 참고. PageHelper가 dialect 설정에 따라 자동으로 DB별 페이징을 처리하므로 ROWNUM, LIMIT 등을 SQL에 직접 작성하지 않는다.

---

## 5. DB별 SQL 문법 차이 요약

| 기능 | Oracle | MySQL | 공통화 방법 |
| --- | --- | --- | --- |
| 문자열 결합 | `\|\|` | `CONCAT()` | databaseId 분기 |
| NULL 대체 | `NVL()` | `IFNULL()` | databaseId 분기 |
| Batch Insert | `DUAL + UNION ALL` | `VALUES (...), (...)` | databaseId 분기 |
| 페이징 | `ROWNUM` | `LIMIT` | PageHelper 자동 처리 |
| 현재 시간 | `SYSDATE` | `NOW()` | databaseId 분기 |
| 시퀀스 | `SEQ.NEXTVAL` | `AUTO_INCREMENT` | databaseId 분기 |
| 타입 변환 | `TO_CHAR()` / `TO_NUMBER()` | `CAST()` | databaseId 분기 |
| 빈 SELECT | `FROM DUAL` 필요 | 불필요 | databaseId 분기 |
| 스키마 지정 | `ALTER SESSION SET CURRENT_SCHEMA` | DB명을 URL에 포함 | application.yml |

---

## 6. 체크리스트

DB 전환 시 확인 항목:

- [ ]  `spring.profiles.active` 변경
- [ ]  `application-{db}.yml` 설정 완료 (DataSource, HikariCP)
- [ ]  DatabaseIdProvider Bean 등록 확인
- [ ]  모든 DB 전용 SQL에 `databaseId` 지정 확인
- [ ]  PageHelper `helper-dialect` 설정 확인
- [ ]  DDL 스크립트로 테이블/초기 데이터 생성 완료
- [ ]  기본 CRUD + 주요 조회 화면 동작 검증 완료

---

*Last updated: 2026-02-23*
