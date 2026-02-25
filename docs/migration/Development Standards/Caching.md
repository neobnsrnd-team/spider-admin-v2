# Caching

## 1. 개요

Spring Boot + Caffeine 기반 캐싱 어노테이션 표준을 정의한다.

---

## 2. 캐시 설정

### 2.1 의존성

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 2.2 @EnableCaching 활성화

Application 클래스 또는 별도 설정 클래스에서 `@EnableCaching`을 **1곳에서만** 선언한다.

```java
@SpringBootApplication
@EnableCaching
public class Application { ... }
```

---

## 3. 캐싱 어노테이션 종류 및 용도

| 어노테이션 | 용도 | 사용 시점 |
| --- | --- | --- |
| `@Cacheable` | 캐시 조회, 없으면 실행 후 저장 | 반복 조회가 잦고 변경이 드문 데이터 |
| `@CacheEvict` | 캐시 삭제 | 데이터 변경(생성/수정/삭제) 시 |
| `@CachePut` | 항상 실행 후 결과를 캐시에 저장 | 실행 결과를 항상 최신으로 캐시해야 할 때 |
| `@Caching` | 여러 캐시 연산을 한 메서드에 조합 | 하나의 변경이 여러 캐시에 영향을 줄 때 |

### 3.1 @Cacheable 작성 규칙

```java
@Cacheable(
    value = "캐시이름",                    // 필수: 캐시 이름
    key = "#파라미터 기반 키",              // 필수: 캐시 키
    condition = "#조건",                  // 선택: 캐시 저장 조건
    unless = "#result == null"           // 선택: 결과 기반 제외 조건
)
public ReturnType methodName(ParamType param) {
    // DB 조회 등 비용이 큰 작업
}
```

**예시:**

```java
// 단일 키
@Cacheable(value = CacheNames.USER_CONFIG, key = "#userId + ':' + #configKey")
public String getUserConfig(String userId, String configKey) { ... }

// 객체 키
@Cacheable(value = CacheNames.CODE_LIST, key = "#groupId")
public List<CodeResponseDTO> getCodesByGroup(String groupId) { ... }

// null 결과 캐시 방지
@Cacheable(value = CacheNames.CODE_LIST, key = "#groupId", unless = "#result == null")
public List<CodeResponseDTO> getCodesByGroupSafe(String groupId) { ... }
```

### 3.2 @CacheEvict 작성 규칙

데이터가 변경되면 **반드시 관련 캐시를 무효화**한다.

```java
// 특정 키 삭제
@CacheEvict(value = CacheNames.USER_CONFIG, key = "#userId + ':' + #configKey")
public void updateUserConfig(String userId, String configKey, String value) { ... }

// 전체 삭제
@CacheEvict(value = CacheNames.CODE_LIST, allEntries = true)
public void updateCode(CodeUpdateRequestDTO dto) { ... }
```

> **규칙:** `@Cacheable`이 있으면 관련 변경 메서드에 반드시 `@CacheEvict`를 짝으로 작성한다.
>

### 3.3 @CachePut 작성 규칙

```java
@CachePut(value = CacheNames.CODE_LIST, key = "#dto.groupId")
public List<CodeResponseDTO> updateAndRefreshCodes(CodeUpdateRequestDTO dto) {
    // 수정 실행 후 최신 목록 반환 → 이 반환값이 캐시에 저장됨
    return updatedList;
}
```

> **주의:** @CachePut은 메서드를 항상 실행한다. 대부분의 경우 @CacheEvict로 충분하며,
@CachePut은 수정 직후 최신 값을 캐시에 넣어야 하는 특수한 경우에만 사용한다.
>

### 3.4 @Caching 조합 사용

하나의 변경이 **서로 다른 여러 캐시**에 영향을 줄 때만 사용한다.

```java
@Caching(
    evict = {
        @CacheEvict(value = CacheNames.CODE_LIST, key = "#groupId"),
        @CacheEvict(value = CacheNames.CODE_GROUP_LIST, allEntries = true)
    }
)
public void updateCodeGroup(String groupId) { ... }
```

> 단일 캐시에 대한 단일 연산이면 `@CacheEvict` 하나로 충분하다. `@Caching`으로 감싸지 않는다.
>

---

## 4. 캐시 이름 표준

### 4.1 네이밍 규칙

```
{도메인}{대상}{유형}
```

| 예시 | 설명 |
| --- | --- |
| `userConfig` | 사용자 설정 |
| `codeList` | 코드 목록 |
| `codeGroupList` | 코드그룹 목록 |
| `menuTree` | 메뉴 트리 |
| `roleList` | 역할 목록 |

### 4.2 캐시 이름 상수 관리

캐시 이름은 문자열 하드코딩 대신 **상수 클래스**로 관리한다.

```java
public final class CacheNames {
    public static final String USER_CONFIG = "userConfig";
    public static final String CODE_LIST = "codeList";
    public static final String CODE_GROUP_LIST = "codeGroupList";
    public static final String MENU_TREE = "menuTree";
    public static final String ROLE_LIST = "roleList";

    private CacheNames() {}
}
```

**사용:**

```java
@Cacheable(value = CacheNames.USER_CONFIG, key = "#userId + ':' + #configKey")
```

---

## 5. CacheManager 설정 표준

### 5.1 Caffeine CacheManager 설정

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            buildCache(CacheNames.USER_CONFIG, 30, 500),
            buildCache(CacheNames.CODE_LIST, 60, 200),
            buildCache(CacheNames.CODE_GROUP_LIST, 60, 100),
            buildCache(CacheNames.MENU_TREE, 60, 50)
        ));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, int expireMinutes, int maxSize) {
        return new CaffeineCache(name,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(expireMinutes))
                .maximumSize(maxSize)
                .recordStats()          // 캐시 통계 수집 (모니터링용)
                .build()
        );
    }
}
```

> `@EnableCaching`이 Application 클래스에 있으면 CacheConfig에서는 생략한다. **중복 선언 금지.**
>

### 5.2 TTL 설정 원칙

| 변경 빈도 | TTL 권장값 | 예시 |
| --- | --- | --- |
| 거의 불변 (코드, 메뉴) | 60분 | codeList, menuTree |
| 드물게 변경 (설정, 역할) | 30분 | userConfig, roleList |
| 자주 변경 (사용자 정보) | 10분 이하 또는 캐시 미적용 | - |

> **규칙:** TTL이 없는 캐시는 금지한다. 모든 캐시에 만료 시간과 최대 크기를 설정한다.
>

---

## 6. 캐시 사용 위치 표준

### 6.1 캐시 적용 위치

```
Controller → Service → Mapper
                ↑
            여기서 캐시 적용
```

> **규칙:** 캐시 어노테이션은 **Service 계층**에서만 사용한다.
Controller나 Mapper에서 캐시하지 않는다.
>

### 6.2 캐시 적합 대상

| 적합 | 부적합 |
| --- | --- |
| 자주 조회되고 변경이 드문 데이터 | 자주 변경되는 데이터 |
| 계산 비용이 높은 결과 | 실시간성이 중요한 데이터 |
| 전체 사용자가 공유하는 데이터 (코드, 메뉴) | 매번 다른 결과를 반환하는 데이터 |
| 사용자별이라도 반복 조회가 잦은 데이터 (설정 등) | 단건 엔티티 (변경 빈도 높음) |

---

## 7. 캐시 무효화 전략

### 7.1 기본 원칙

데이터 변경 시 관련 캐시를 **즉시 무효화**한다.

```java
// 생성 → 목록 캐시 무효화
@CacheEvict(value = CacheNames.CODE_LIST, allEntries = true)
@Transactional
public CodeResponseDTO createCode(CodeCreateRequestDTO dto) { ... }

// 수정 → 목록 캐시 무효화
@CacheEvict(value = CacheNames.CODE_LIST, allEntries = true)
@Transactional
public CodeResponseDTO updateCode(CodeUpdateRequestDTO dto) { ... }

// 삭제 → 목록 캐시 무효화
@CacheEvict(value = CacheNames.CODE_LIST, allEntries = true)
@Transactional
public void deleteCode(String codeId) { ... }
```

### 7.2 @Cacheable - @CacheEvict 매핑 원칙

| @Cacheable 메서드 | @CacheEvict 메서드 |
| --- | --- |
| 목록 조회 (getList) | 생성, 수정, 삭제 (allEntries=true) |
| 단건 조회 (getById) | 수정, 삭제 (해당 키) |
| 트리 조회 (getTree) | 노드 생성, 수정, 삭제, 이동 (allEntries=true) |

> **규칙:** @Cacheable을 추가할 때 반드시 관련 변경 메서드의 @CacheEvict를 같이 작성한다.
>

---

## 8. 주의사항

### 8.1 프록시 기반 한계

Spring AOP 프록시 방식이므로 **같은 클래스 내부 호출은 캐시가 동작하지 않는다.**

```java
@Service
public class CodeServiceImpl implements CodeService {

    // ❌ 내부 호출: 캐시 미작동
    public void doSomething() {
        this.getCodesByGroup("GRP01");     // 캐시 무시됨
    }

    @Cacheable(value = CacheNames.CODE_LIST, key = "#groupId")
    public List<CodeResponseDTO> getCodesByGroup(String groupId) { ... }
}
```

> **해결:** 캐시 메서드를 별도 서비스로 분리하거나, 외부에서 호출하도록 구성한다.
>

### 8.2 @Transactional과 함께 사용

```java
@CacheEvict(value = CacheNames.CODE_LIST, allEntries = true)
@Transactional
public void updateCode(CodeUpdateRequestDTO dto) { ... }
```

> **주의:** `@CacheEvict`는 메서드 반환 후 실행되지만(기본 `beforeInvocation=false`),
트랜잭션 커밋 시점과의 순서는 Spring AOP 프록시 순서에 의존한다.
트랜잭션 롤백 시에도 캐시가 이미 삭제될 수 있으므로, 캐시 불일치가 문제되면
TTL에 의한 자연 만료로 보완한다.
>

### 8.3 null 캐싱 방지

```java
// null 결과는 캐시하지 않음
@Cacheable(value = CacheNames.USER_CONFIG, key = "#userId + ':' + #configKey",
        unless = "#result == null")
public String getUserConfig(String userId, String configKey) { ... }
```

---

## 9. 체크리스트

캐시 어노테이션 추가 시 확인 항목:

- [ ]  Service 계층에서만 캐시 어노테이션을 사용했는가
- [ ]  캐시 이름을 CacheNames 상수로 관리하는가
- [ ]  CacheManager에 해당 캐시의 TTL과 최대 크기가 설정되어 있는가
- [ ]  @Cacheable에 대응하는 @CacheEvict가 변경 메서드에 존재하는가
- [ ]  null 결과에 대해 unless 조건을 설정했는가
- [ ]  같은 클래스 내부 호출이 아닌지 확인했는가
- [ ]  @EnableCaching이 프로젝트 내 1곳에서만 선언되어 있는가

---

*Last updated: 2026-02-23*