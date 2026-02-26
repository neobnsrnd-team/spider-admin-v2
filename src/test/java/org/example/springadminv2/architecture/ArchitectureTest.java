package org.example.springadminv2.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureTest {

    private static final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.example.springadminv2");

    // ── 레이어 의존성: Controller → Service → Mapper 단방향만 허용 ──

    @Test
    void layer_dependencies() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller")
                .definedBy("..controller..")
                .layer("Service")
                .definedBy("..service..")
                .layer("Mapper")
                .definedBy("..mapper..")
                .whereLayer("Controller")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                .mayOnlyBeAccessedByLayers("Controller", "Service")
                .whereLayer("Mapper")
                .mayOnlyBeAccessedByLayers("Service")
                .allowEmptyShould(true)
                .check(classes);
    }

    // ── 금지 클래스명 ──

    @Test
    void no_entity_classes() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .haveSimpleNameEndingWith("Entity")
                .because("Entity 클래스를 사용하지 않는다 — MyBatis ResultMap이 DTO에 직접 매핑")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void no_converter_classes() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .haveSimpleNameEndingWith("Converter")
                .because("Converter 클래스를 사용하지 않는다 — ResultMap 직접 매핑")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void no_service_impl() {
        noClasses()
                .that()
                .resideInAPackage("..service..")
                .should()
                .haveSimpleNameEndingWith("ServiceImpl")
                .because("인터페이스 + Impl 패턴을 사용하지 않는다 — 구체 클래스만 허용")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void no_vo_classes() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .haveSimpleNameEndingWith("VO")
                .because("VO 클래스를 사용하지 않는다 — DTO로 통일")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void no_dto_suffix() {
        noClasses()
                .that()
                .resideInAPackage("..dto..")
                .should()
                .haveSimpleNameEndingWith("DTO")
                .because("DTO suffix를 사용하지 않는다 — Request/Response 등 역할 기반 이름 사용")
                .allowEmptyShould(true)
                .check(classes);
    }

    // ── DTO는 Java Record ──

    @Test
    void dto_should_be_records() {
        classes()
                .that()
                .resideInAPackage("..dto..")
                .and()
                .areTopLevelClasses()
                .and()
                .doNotHaveSimpleName("ApiResponse")
                .and()
                .doNotHaveSimpleName("ErrorDetail")
                .should()
                .beAssignableTo(Record.class)
                .because("DTO는 Java Record로 작성한다 — ApiResponse, ErrorDetail 제외")
                .allowEmptyShould(true)
                .check(classes);
    }

    // ── DI 규칙 ──

    @Test
    void no_autowired() {
        noFields()
                .should()
                .beAnnotatedWith(Autowired.class)
                .because("@RequiredArgsConstructor + private final을 사용한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    // ── 컨트롤러 이름 컨벤션 ──

    @Test
    void rest_controller_naming() {
        noClasses()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .haveSimpleNameEndingWith("RestController")
                .because("REST 컨트롤러는 {Domain}Controller로 명명한다 — RestController 접미사 금지")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void page_controller_naming() {
        classes()
                .that()
                .areAnnotatedWith(Controller.class)
                .and()
                .areNotAnnotatedWith(RestController.class)
                .and()
                .resideInAPackage("..controller..")
                .should()
                .haveSimpleNameEndingWith("PageController")
                .because("Page 컨트롤러는 {Domain}PageController로 명명한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    // ── global.web → Mapper 직접 의존 금지 ──

    @Test
    void global_web_should_not_access_mapper() {
        noClasses()
                .that()
                .resideInAPackage("..global.web..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..mapper..")
                .because("global.web 컨트롤러도 Mapper 직접 의존을 금지한다 — Service를 통해 접근")
                .allowEmptyShould(true)
                .check(classes);
    }
}
