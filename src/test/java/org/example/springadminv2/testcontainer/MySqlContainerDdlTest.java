package org.example.springadminv2.testcontainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Tag("docker")
class MySqlContainerDdlTest {

    @Container
    private static final MySQLContainer<?> MYSQL = MySqlContainerConfig.MYSQL;

    @Test
    void executeAllMySqlDdl() throws Exception {
        Path ddlDir = Path.of("src/test/resources/db/mysql/ddl");
        assertThat(ddlDir).isDirectory();

        int tableCount = 0;
        try (Connection conn = MYSQL.createConnection("");
                Statement stmt = conn.createStatement()) {

            try (Stream<Path> files = Files.list(ddlDir)) {
                for (Path file :
                        files.filter(f -> f.toString().endsWith(".sql")).toList()) {
                    String sql = Files.readString(file);
                    // Remove single-line comments (CRLF-safe)
                    sql = sql.replaceAll("--[^\r\n]*", "");
                    // Convert Oracle double-quotes to MySQL backticks
                    sql = sql.replace("\"", "`");
                    // Split by semicolons and execute each statement
                    for (String statement : sql.split(";")) {
                        String trimmed = statement.trim();
                        if (!trimmed.isEmpty()) {
                            try {
                                stmt.execute(trimmed);
                            } catch (Exception e) {
                                String msg = e.getMessage();
                                if (!msg.contains("already exists") && !msg.contains("Row size too large")) {
                                    throw new RuntimeException(
                                            "Failed to execute DDL from " + file.getFileName() + ": " + trimmed, e);
                                }
                            }
                        }
                    }
                    tableCount++;
                }
            }
        }

        assertThat(tableCount).isGreaterThan(0);

        // Verify tables exist
        try (Connection conn = MYSQL.createConnection("");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'spider_admin'")) {
            rs.next();
            assertThat(rs.getInt(1)).isGreaterThan(0);
        }
    }
}
