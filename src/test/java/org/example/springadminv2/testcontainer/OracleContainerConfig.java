package org.example.springadminv2.testcontainer;

import org.testcontainers.containers.OracleContainer;

public final class OracleContainerConfig {

    private OracleContainerConfig() {}

    static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-xe:21-slim").withReuse(true);

    static {
        ORACLE.start();
    }
}
