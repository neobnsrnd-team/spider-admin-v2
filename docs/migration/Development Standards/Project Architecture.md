# Project Architecture

## 1. 개요

프로젝트의 레이어 구조, 데이터 흐름, 의존성 규칙, 패키지 구조를 정의한다.

핵심 원칙:

- **단방향 의존**: Controller → Service → Mapper → DB
- **Entity 없음**: MyBatis ResultMap이 DTO에 직접 매핑한다
- **생성자 주입 전용**: `@RequiredArgsConstructor` + `private final`

---

## 2. 레이어 구조

### 2.1 레이어 다이어그램

```
Client → Controller → Service → Mapper → DB
           (DTO)       (DTO)    (XML)
```

| 레이어 | 역할 | 허용 의존 |
|--------|------|-----------|
| Controller | HTTP 요청/응답 매핑 | Service |
| Service | 비즈니스 로직, 트랜잭션 | Mapper, 다른 도메인 Service |
| Mapper | SQL 실행 (MyBatis interface) | — (XML에서 SQL 정의) |

### 2.2 금지 의존

| 금지 | 이유 |
|------|------|
| Controller → Mapper | Service 레이어 우회 |
| Controller → Controller | 계층 역전 |
| Mapper → Service | 역방향 의존 |
| Service → Controller | 역방향 의존 |

---

## 3. 데이터 흐름

### 3.1 Entity를 사용하지 않는 이유

MyBatis는 SQL 결과를 Record DTO에 직접 매핑할 수 있다 (`arg-name-based-constructor-auto-mapping` 활성화). 별도 Entity 클래스를 거치면 Entity ↔ DTO 변환 코드가 불필요하게 증가한다. DTO는 Java Record로 작성하여 불변성을 보장한다.

```
DB → resultType/ResultMap → ResponseDTO (Record 직접 매핑)
```

> 이 결정으로 `entity/`, `converter/`, `model/`, `vo/` 패키지가 불필요하다.

### 3.2 조회 (Read)

```
Controller ← ResponseDTO ← Service ← ResponseDTO ← Mapper(ResultMap)
```

Mapper의 `resultType` 또는 `resultMap`이 ResponseDTO에 직접 매핑한다.

> Mapper에서 DTO 직접 매핑은 SQL 5.4절 참고.

### 3.3 생성/수정 (Write)

```
Controller → RequestDTO → Service → Mapper(parameterType=RequestDTO or Map)
```

Service에서 RequestDTO를 Mapper에 직접 전달하거나, 필요 시 Map/파라미터로 변환한다.

### 3.4 삭제 (Delete)

```
Controller → id → Service → Mapper(id)
```

---

## 4. 의존성 규칙

### 4.1 동일 도메인

```
domain/user/controller/ → domain/user/service/ → domain/user/mapper/   ✅
```

### 4.2 크로스 도메인

**Service → Service만 허용한다.** Controller나 Mapper 간 직접 참조는 금지한다.

```
domain/order/service/OrderService → domain/user/service/UserService     ✅
domain/order/controller/ → domain/user/service/                         ❌
domain/order/mapper/ → domain/user/mapper/                              ❌
```

### 4.3 순환 의존

순환 의존(A → B → A)이 발생하면 공통 로직을 `global/` 패키지로 추출한다.

---

## 5. 의존성 주입

### 5.1 표준 패턴

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final RoleService roleService;   // 크로스 도메인: Service → Service
}
```

### 5.2 금지 패턴

| 금지 | 대안 |
|------|------|
| `@Autowired` 필드 주입 | `@RequiredArgsConstructor` + `private final` |
| `@Inject` | `@RequiredArgsConstructor` + `private final` |
| setter 주입 | `@RequiredArgsConstructor` + `private final` |

---

## 6. Service 규칙

### 6.1 인터페이스 없이 구체 클래스만

```java
// ✅
@Service
public class UserService { ... }

// ❌ 인터페이스 + Impl
public interface UserService { ... }
public class UserServiceImpl implements UserService { ... }
```

내부 소비자만 존재하므로 인터페이스 추상화가 불필요하다.

### 6.2 트랜잭션

```java
@Service
@Transactional(readOnly = true)          // 클래스 레벨: 조회 기본
@RequiredArgsConstructor
public class UserService {

    @Transactional                        // 쓰기 메서드만 오버라이드
    public UserResponseDTO create(UserCreateRequestDTO dto) { ... }
}
```

---

## 7. 패키지 구조

### 7.1 전체 레이아웃

```
src/main/java/{base-package}/
├── domain/                          # 도메인별 비즈니스 로직
│   ├── user/
│   │   ├── controller/
│   │   │   ├── UserController.java
│   │   │   └── UserPageController.java
│   │   ├── service/
│   │   │   └── UserService.java
│   │   ├── mapper/
│   │   │   └── UserMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── UserCreateRequestDTO.java
│   │       │   └── UserUpdateRequestDTO.java
│   │       └── response/
│   │           ├── UserResponseDTO.java
│   │           └── UserDetailResponseDTO.java
│   └── order/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       └── dto/
│
├── global/                          # 도메인 공통/횡단 관심사
│   ├── config/                      # @Configuration
│   ├── exception/                   # Exception Handling 4절
│   ├── dto/                         # ApiResponse, PageRequest 등
│   ├── security/                    # Security, Auth & Auth
│   ├── filter/                      # 서블릿 필터
│   ├── interceptor/                 # 인터셉터
│   ├── typehandler/                 # MyBatis TypeHandler (Code Convention 5절)
│   └── util/                        # 유틸리티 (최소화)
│
└── resources/
    └── mapper/                      # MyBatis XML
        ├── user/
        │   └── UserMapper.xml
        └── order/
            └── OrderMapper.xml
```

### 7.2 금지 패키지

| 금지 패키지 | 이유 |
|------------|------|
| `entity/` | Entity 클래스 불사용 (3.1절) |
| `converter/` | Converter 클래스 불사용 (3.1절) |
| `model/` | Entity/VO의 변형 — DTO로 통일 |
| `vo/` | DTO로 통일 |
| `repository/` | JPA 불사용 — Mapper로 통일 |
| `impl/` | ServiceImpl 패턴 불사용 (6.1절) |

---

## 8. ArchUnit 테스트

### 8.1 의존성

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

### 8.2 레이어 규칙

```java
@AnalyzeClasses(packages = "org.example.springadminv2")
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("org.example.springadminv2");

    @Test
    void layer_dependencies() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Mapper").definedBy("..mapper..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
                .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Service")
                .check(classes);
    }
}
```

### 8.3 금지 클래스 규칙

```java
@Test
void no_entity_classes() {
    noClasses().that().resideInAPackage("..domain..")
            .should().haveSimpleNameEndingWith("Entity")
            .check(classes);
}

@Test
void no_converter_classes() {
    noClasses().that().resideInAPackage("..domain..")
            .should().haveSimpleNameEndingWith("Converter")
            .check(classes);
}

@Test
void no_service_impl() {
    noClasses().that().resideInAPackage("..service..")
            .should().haveSimpleNameEndingWith("ServiceImpl")
            .check(classes);
}

@Test
void no_vo_classes() {
    noClasses().that().resideInAPackage("..domain..")
            .should().haveSimpleNameEndingWith("VO")
            .check(classes);
}

@Test
void dto_should_be_records() {
    classes().that().haveSimpleNameEndingWith("DTO")
            .should().beRecords()
            .check(classes);
}
```

### 8.4 DI 규칙

```java
@Test
void no_autowired() {
    noFields().should().beAnnotatedWith(Autowired.class)
            .because("@RequiredArgsConstructor + private final을 사용한다")
            .check(classes);
}
```

---

## 9. 체크리스트

```
□ 1. 의존 방향이 Controller → Service → Mapper인지 확인
      → 역방향 참조 없음

□ 2. Entity/Converter/Model/VO 클래스 없음
      → ResultMap으로 DTO 직접 매핑

□ 3. Service는 구체 클래스만 (인터페이스 + Impl 금지)

□ 4. DI는 @RequiredArgsConstructor + private final
      → @Autowired, @Inject 미사용

□ 5. 크로스 도메인 참조는 Service → Service만

□ 6. 패키지 구조: domain/ + global/ 이외 최상위 패키지 없음

□ 7. ArchUnit 테스트 통과
```

---

*Last updated: 2026-02-26*
