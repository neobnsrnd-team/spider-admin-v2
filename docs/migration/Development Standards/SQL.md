# SQL

## 1. 개요

Spring Boot + MyBatis 환경에서의 SQL 작성 표준을 정의한다. 멀티 DB SQL 분기 전략은 Multi-DB 문서 참고.

---

## 2. MyBatis 설정

### 2.1 공통 설정

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.dto
  configuration:
    map-underscore-to-camel-case: true      # DB: USER_NAME → Java: userName 자동 매핑
    jdbc-type-for-null: NULL                # null 파라미터의 JDBC 타입
    lazy-loading-enabled: false             # 지연 로딩 비활성화
    cache-enabled: true                     # 2단계 캐시 활성화
    auto-mapping-behavior: PARTIAL          # 명시된 컬럼만 매핑
```

### 2.2 PageHelper 설정

```yaml
pagehelper:
  helper-dialect: oracle                    # oracle, mysql, postgresql
  reasonable: true                          # pageNum < 1 → 1, pageNum > 총페이지 → 마지막
  support-methods-arguments: true
  params: count=countSql
```

> DB 전환 시 `helper-dialect`만 변경하면 된다. → Multi-DB 2절 참고

---

## 3. Mapper 파일 구조

도메인별로 **Mapper XML 1개**로 관리한다.

```
resources/mapper/{domain}/
└── {Entity}Mapper.xml           ← CRUD + 조회 + 검색 + ResultMap + Fragment 전부
```

| 구성 요소 | 위치 | 비고 |
| --- | --- | --- |
| ResultMap | Mapper XML 상단 | `<resultMap>` 정의 |
| SQL Fragment | ResultMap 아래 | `<sql>` (3곳 이상 반복 시만) |
| CRUD SQL | Fragment 아래 | `insert*`, `update*`, `delete*`, `selectById` |
| 조회/검색 SQL | CRUD 아래 | `findAll*`, `findBy*`, `count*` |

> **원칙:** 하나의 Mapper에 모든 SQL을 관리하여 복잡도를 줄인다.
>

### 3.1 네임스페이스 규칙

```xml
<mapper namespace="com.example.domain.{domain}.mapper.{Entity}Mapper">
```

> 네임스페이스는 Java Mapper 인터페이스의 FQCN과 **정확히 일치**해야 한다.
>

---

## 4. SQL ID 네이밍 표준

| 접두사 | 용도 | 예시 |
| --- | --- | --- |
| `selectById` | PK 단건 조회 | `selectById` |
| `insert{Entity}` | 단건 생성 | `insertUser`, `insertOrder` |
| `insertBatch` | 배치 생성 | `insertBatch` |
| `update{Entity}` | 단건 수정 | `updateUser`, `updateOrder` |
| `delete{Entity}ById` | 단건 삭제 | `deleteUserById` |
| `findAll` | 전체 목록 | `findAll` |
| `findAllWithSearch` | 검색 조건 목록 | `findAllWithSearch` |
| `findBy{Field}` | 특정 필드로 조회 | `findByUserId`, `findByEmail` |
| `count{Field}` | 카운트 | `countByUserName`, `countAll` |
| `findDistinct{Field}` | 고유값 목록 | `findDistinctStatus` |

### 4.1 금지 패턴

```xml
<!-- ❌ 금지: 모호한 이름 -->
<select id="getList">
<select id="getData">
<select id="select1">

<!-- ✅ 권장: 명확한 의도 -->
<select id="findAllWithSearch">
<select id="findByUserId">
<select id="countByUserName">
```

---

## 5. ResultMap 표준

### 5.1 정의 위치

ResultMap은 **Mapper XML 상단**에 정의한다.

### 5.2 네이밍 규칙

| 패턴 | 용도 | 예시 |
| --- | --- | --- |
| `Base{Entity}ResultMap` | 엔티티 기본 매핑 | `BaseUserResultMap` |

> 조인 결과는 ResultMap을 만들지 않고 `resultType="DTO"`로 직접 매핑한다. (5.4 참조)
>

### 5.3 작성 예시

```xml
<!-- Mapper XML 상단에 정의 -->
<resultMap id="BaseUserResultMap" type="com.example.domain.user.entity.User">
    <id property="userId" column="USER_ID"/>
    <result property="userName" column="USER_NAME"/>
    <result property="email" column="EMAIL"/>
    <result property="roleId" column="ROLE_ID"/>          <!-- FK값만, 객체 참조 금지 -->
    <result property="status" column="STATUS"
            typeHandler="com.example.global.handler.StatusTypeHandler"/>
    <result property="lastUpdateDtime" column="LAST_UPDATE_DTIME"/>
</resultMap>
```

**금지 사항:**

- Entity ResultMap에서 `<association>`, `<collection>`은 사용하지 않는다.
- 관련 데이터가 필요하면 조인 조회 + DTO 직접 매핑으로 해결한다.

### 5.4 조인 결과 매핑 (DTO 직접 매핑)

```xml
<select id="findAllWithSearch" resultType="UserDetailResponseDTO">
    SELECT u.USER_ID     AS userId,
           u.USER_NAME   AS userName,
           r.ROLE_NAME   AS roleName        <!-- 조인 컬럼은 DTO 필드에 직접 매핑 -->
    FROM USERS u
    LEFT JOIN ROLES r ON u.ROLE_ID = r.ROLE_ID
</select>
```

> `map-underscore-to-camel-case: true` 설정으로 `USER_NAME` → `userName` 자동 변환.
>

---

## 6. SQL Fragment 표준

### 6.1 사용 원칙

SQL Fragment(`<sql>`)는 **3곳 이상에서 동일하게 반복되는 경우에만** 사용한다.
동적 SQL이 대부분인 환경에서 무리하게 Fragment로 분리하면 오히려 가독성이 떨어진다.

| 적합 | 부적합 |
| --- | --- |
| 3곳 이상에서 동일하게 반복되는 컬럼/조건 | 한두 곳에서만 쓰는 SQL |
| ResultMap에서 참조하는 기본 컬럼 목록 | 동적 조건이 많아 재사용이 어려운 SQL |
| - | FROM/JOIN 절 (조회마다 조인 구성이 다름) |

### 6.2 권장 Fragment

```xml
<!-- ✅ 권장: 여러 곳에서 반복되는 기본 컬럼 -->
<sql id="baseColumns">
    u.USER_ID, u.USER_NAME, u.EMAIL, u.STATUS,
    u.LAST_UPDATE_DTIME
</sql>
```

```xml
<!-- ❌ 비권장: 조회마다 조인이 다른데 억지로 Fragment화 -->
<sql id="fromUserWithRole">...</sql>
<sql id="fromUserWithRoleAndMenu">...</sql>
```

> **규칙:** Fragment를 만들기 전에 "이걸 3곳 이상에서 그대로 쓰는가?"를 먼저 확인한다.
>

### 6.3 참조 방법

```xml
<!-- 같은 Mapper 내 -->
<include refid="baseColumns"/>

<!-- 다른 Mapper의 Fragment 참조 -->
<include refid="com.example.domain.user.mapper.UserMapper.baseColumns"/>
```

> DB별 문법 차이(LIKE 등)는 Fragment가 아닌 `databaseId` 분기로 처리한다. → Multi-DB 3절 참고

---

## 7. 동적 SQL 표준

### 7.1 WHERE 조건

```xml
<!-- ✅ <where> 태그 사용: 첫 번째 AND/OR 자동 제거 -->
<select id="findAllWithSearch" resultType="UserResponseDTO">
    SELECT u.USER_ID, u.USER_NAME, r.ROLE_NAME
    FROM USERS u
    LEFT JOIN ROLES r ON u.ROLE_ID = r.ROLE_ID
    <where>
        <if test="searchValue != null and searchValue != ''">
            <choose>
                <when test="searchField == 'userName'">
                    AND u.USER_NAME LIKE '%' || #{searchValue} || '%'
                </when>
                <when test="searchField == 'userId'">
                    AND u.USER_ID LIKE '%' || #{searchValue} || '%'
                </when>
            </choose>
        </if>
        <if test="roleFilter != null and roleFilter != ''">
            AND u.ROLE_ID = #{roleFilter}
        </if>
    </where>
</select>

<!-- ❌ 금지: WHERE 1=1 패턴 -->
WHERE 1=1
<if test="...">AND ...</if>
```

> **참고:** LIKE의 `||` 연산자는 Oracle 문법이다. DB 전환 시 `databaseId` 분기가 필요하다. → Multi-DB 3절 참고

### 7.2 동적 정렬

```xml
ORDER BY
<choose>
    <when test="sortBy == 'userName'">u.USER_NAME</when>
    <when test="sortBy == 'userId'">u.USER_ID</when>
    <otherwise>u.LAST_UPDATE_DTIME</otherwise>
</choose>
<choose>
    <when test="sortDirection == 'ASC'">ASC</when>
    <otherwise>DESC</otherwise>
</choose>
```

> **규칙:** 정렬 컬럼에 `${}` 를 사용하지 않는다. SQL Injection 위험이 있으므로 반드시 `<choose>`로 화이트리스트 방식으로 작성한다.
>

### 7.3 Batch Insert

DB별로 Batch Insert 문법이 다르므로 `databaseId`로 분기한다.

```xml
<!-- Oracle -->
<insert id="insertBatch" databaseId="oracle">
    INSERT INTO ROLE_MENUS (ROLE_ID, MENU_ID, AUTH_CODE)
    SELECT A.* FROM (
        <foreach collection="list" item="item" separator=" UNION ALL ">
            SELECT #{item.roleId}, #{item.menuId}, #{item.authCode}
            FROM DUAL
        </foreach>
    ) A
</insert>

<!-- MySQL -->
<insert id="insertBatch" databaseId="mysql">
    INSERT INTO ROLE_MENUS (ROLE_ID, MENU_ID, AUTH_CODE)
    VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.roleId}, #{item.menuId}, #{item.authCode})
    </foreach>
</insert>
```

> Oracle에서 표준 `VALUES (...), (...)` 문법은 ORA-00933 오류 발생. → Multi-DB 3절 참고

### 7.4 foreach 사용

```xml
<!-- IN 절 -->
<if test="menuIds != null and menuIds.size() > 0">
    AND MENU_ID IN
    <foreach collection="menuIds" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</if>
```

---

## 8. 파라미터 바인딩 표준

### 8.1 기본 바인딩

```xml
<!-- ✅ #{} 사용 (PreparedStatement, SQL Injection 방지) -->
WHERE USER_ID = #{userId}
VALUES (#{userName}, #{email})

<!-- ❌ ${} 금지 (Statement, SQL Injection 취약) -->
WHERE USER_ID = '${userId}'
ORDER BY ${sortColumn}              <!-- 동적 정렬은 6.2의 <choose> 방식 사용 -->
```

### 8.2 JDBC 타입 힌트

NULL 가능 파라미터에는 `jdbcType`을 명시한다.

```xml
#{password, jdbcType=VARCHAR}
#{seq, jdbcType=INTEGER}
#{description, jdbcType=CLOB}
```

### 8.3 Enum TypeHandler

Enum 컬럼은 반드시 TypeHandler를 지정한다.

```xml
<!-- Insert/Update -->
#{status, typeHandler=com.example.global.handler.StatusTypeHandler}

<!-- ResultMap -->
<result property="status" column="STATUS"
        typeHandler="com.example.global.handler.StatusTypeHandler"/>

<!-- Where 조건 -->
AND u.STATUS = #{statusFilter,
    typeHandler=com.example.global.handler.StatusTypeHandler}
```

> **규칙:** Entity/Mapper 계층에서는 Enum, DTO/API 계층에서는 String을 사용한다.
변환은 Converter에서 수행한다.
>

---

## 9. 페이징 표준

### 9.1 PageHelper 사용

```java
// Service 계층에서 호출
PageHelper.startPage(request.getPage(), request.getSize());
List<UserResponseDTO> list = userMapper.findAllWithSearch(params);
return PageResponse.from(list);     // PageInfo → PageResponse 변환
```

| DB | PageHelper 자동 적용 |
| --- | --- |
| Oracle | ROWNUM 방식 |
| MySQL | LIMIT 방식 |
| PostgreSQL | LIMIT OFFSET 방식 |

> **규칙:** SQL에 ROWNUM, LIMIT을 직접 작성하지 않는다. PageHelper에 위임한다.

> `PageRequest`/`PageResponse` 클래스 정의는 API Design 7절 참고.

---

## 10. 체크리스트

새 Mapper XML 작성 시 확인 항목:

- [ ]  도메인별 Mapper XML 1개로 관리하는가
- [ ]  네임스페이스가 Java 인터페이스 FQCN과 일치하는가
- [ ]  SQL ID가 네이밍 규칙을 따르는가 (insert*, find*, count* 등)
- [ ]  ResultMap은 Mapper XML 상단에 정의했는가 (Base ResultMap만)
- [ ]  `<association>`, `<collection>` 을 사용하지 않았는가
- [ ]  조인 결과는 DTO resultType으로 매핑했는가
- [ ]  `<where>` 태그를 사용했는가 (WHERE 1=1 금지)
- [ ]  정렬에 `${}` 대신 `<choose>` 를 사용했는가
- [ ]  Enum 컬럼에 TypeHandler를 지정했는가
- [ ]  페이징은 PageHelper에 위임했는가 (ROWNUM/LIMIT 직접 작성 금지)
- [ ]  DB별 문법 차이가 있는 SQL에 `databaseId`를 지정했는가

---

*Last updated: 2026-02-23*