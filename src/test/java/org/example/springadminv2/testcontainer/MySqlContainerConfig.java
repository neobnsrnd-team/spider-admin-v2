package org.example.springadminv2.testcontainer;

import org.testcontainers.containers.MySQLContainer;

public final class MySqlContainerConfig {

    private MySqlContainerConfig() {}

    public static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("spider_admin").withReuse(true);
}
