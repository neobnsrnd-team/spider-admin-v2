package org.example.springadminv2.security;

import java.util.List;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.example.springadminv2.global.security.dto.AuthenticatedUser;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.example.springadminv2.testcontainer.MySqlContainerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Tag("docker")
@DisplayName("AuthorityMapper — MySQL 통합 테스트")
class AuthorityMapperMySqlTest {

    @Container
    private static final MySQLContainer<?> MYSQL = MySqlContainerConfig.MYSQL;

    private static AuthorityMapper mapper;

    @BeforeAll
    static void setUp() throws Exception {
        DataSource ds = new SimpleDriverDataSource(
                new com.mysql.cj.jdbc.Driver(), MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());

        // DDL
        try (var conn = ds.getConnection();
                var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS FWK_USER ("
                    + "USER_ID VARCHAR(20) PRIMARY KEY, USER_NAME VARCHAR(50) NOT NULL,"
                    + "PASSWORD VARCHAR(50), ROLE_ID VARCHAR(10),"
                    + "USER_STATE_CODE VARCHAR(1), LOGIN_FAIL_COUNT DECIMAL(1,0) DEFAULT 0,"
                    + "PASSWD VARCHAR(100),"
                    + "LAST_UPDATE_DTIME VARCHAR(14) NOT NULL, LAST_UPDATE_USER_ID VARCHAR(20) NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS FWK_USER_MENU ("
                    + "USER_ID VARCHAR(20), MENU_ID VARCHAR(40), AUTH_CODE VARCHAR(1) NOT NULL,"
                    + "LAST_UPDATE_DTIME VARCHAR(14) NOT NULL, LAST_UPDATE_USER_ID VARCHAR(20) NOT NULL,"
                    + "FAVOR_MENU_ORDER DECIMAL(3,0) DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (USER_ID, MENU_ID))");
            stmt.execute("CREATE TABLE IF NOT EXISTS FWK_ROLE_MENU ("
                    + "ROLE_ID VARCHAR(10), MENU_ID VARCHAR(40), AUTH_CODE VARCHAR(1) NOT NULL,"
                    + "PRIMARY KEY (ROLE_ID, MENU_ID))");

            // DML
            stmt.execute("DELETE FROM FWK_USER");
            stmt.execute("DELETE FROM FWK_USER_MENU");
            stmt.execute("DELETE FROM FWK_ROLE_MENU");

            stmt.execute("INSERT INTO FWK_USER (USER_ID,USER_NAME,PASSWD,ROLE_ID,USER_STATE_CODE,LOGIN_FAIL_COUNT,"
                    + "LAST_UPDATE_DTIME,LAST_UPDATE_USER_ID)"
                    + " VALUES ('admin','관리자','{noop}admin','ADMIN','1',0,'20260101000000','system')");
            stmt.execute("INSERT INTO FWK_USER (USER_ID,USER_NAME,PASSWD,ROLE_ID,USER_STATE_CODE,LOGIN_FAIL_COUNT,"
                    + "LAST_UPDATE_DTIME,LAST_UPDATE_USER_ID)"
                    + " VALUES ('user01','일반사용자','{noop}user01','USER','1',0,'20260101000000','system')");

            stmt.execute("INSERT INTO FWK_USER_MENU VALUES ('admin','v3_menu_manage','W','20260101000000','system',0)");
            stmt.execute("INSERT INTO FWK_USER_MENU VALUES ('admin','v3_role_manage','W','20260101000000','system',0)");
            stmt.execute(
                    "INSERT INTO FWK_USER_MENU VALUES ('user01','v3_menu_manage','R','20260101000000','system',0)");

            stmt.execute("INSERT INTO FWK_ROLE_MENU VALUES ('ADMIN','v3_menu_manage','W')");
            stmt.execute("INSERT INTO FWK_ROLE_MENU VALUES ('USER','v3_menu_manage','R')");
        }

        // MyBatis
        SqlSessionFactoryBean fb = new SqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/mysql/security/AuthorityMapper.xml"));
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        fb.setConfiguration(config);
        SqlSessionFactory sf = fb.getObject();

        MapperFactoryBean<AuthorityMapper> mfb = new MapperFactoryBean<>(AuthorityMapper.class);
        mfb.setSqlSessionFactory(sf);
        mapper = mfb.getObject();
    }

    @Test
    @DisplayName("selectMenuPermissionsByUserId — admin 사용자 권한 조회")
    void selectMenuPermissionsByUserId() {
        List<MenuPermission> perms = mapper.selectMenuPermissionsByUserId("admin");

        assertThat(perms).hasSize(2);
        assertThat(perms)
                .extracting(MenuPermission::menuId)
                .containsExactlyInAnyOrder("v3_menu_manage", "v3_role_manage");
    }

    @Test
    @DisplayName("selectMenuPermissionsByRoleId — ADMIN 역할 권한 조회")
    void selectMenuPermissionsByRoleId() {
        List<MenuPermission> perms = mapper.selectMenuPermissionsByRoleId("ADMIN");

        assertThat(perms).hasSize(1);
        assertThat(perms.get(0).menuId()).isEqualTo("v3_menu_manage");
        assertThat(perms.get(0).authCode()).isEqualTo("W");
    }

    @Test
    @DisplayName("selectUserById — 사용자 조회")
    void selectUserById() {
        AuthenticatedUser user = mapper.selectUserById("admin");

        assertThat(user).isNotNull();
        assertThat(user.userId()).isEqualTo("admin");
        assertThat(user.roleId()).isEqualTo("ADMIN");
        assertThat(user.password()).isEqualTo("{noop}admin");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 → null")
    void selectUserById_not_found() {
        AuthenticatedUser user = mapper.selectUserById("noone");
        assertThat(user).isNull();
    }

    @Test
    @DisplayName("권한 없는 사용자 → 빈 리스트")
    void no_permissions() {
        List<MenuPermission> perms = mapper.selectMenuPermissionsByUserId("noone");
        assertThat(perms).isEmpty();
    }
}
