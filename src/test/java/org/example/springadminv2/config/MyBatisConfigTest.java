package org.example.springadminv2.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.example.springadminv2.global.config.MyBatisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyBatisConfigTest {

    private final MyBatisConfig config = new MyBatisConfig();

    @Test
    @DisplayName("DatabaseIdProvider Bean이 정상 생성된다")
    void databaseIdProvider_isNotNull() {
        DatabaseIdProvider provider = config.databaseIdProvider();

        assertThat(provider).isNotNull();
    }
}
