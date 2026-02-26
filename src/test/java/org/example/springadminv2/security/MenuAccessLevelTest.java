package org.example.springadminv2.security;

import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuAccessLevelTest {

    @Test
    void fromCode_read() {
        assertThat(MenuAccessLevel.fromCode("R")).isEqualTo(MenuAccessLevel.READ);
    }

    @Test
    void fromCode_write() {
        assertThat(MenuAccessLevel.fromCode("W")).isEqualTo(MenuAccessLevel.WRITE);
    }

    @Test
    void fromCode_unknown_throws() {
        assertThatThrownBy(() -> MenuAccessLevel.fromCode("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }
}
