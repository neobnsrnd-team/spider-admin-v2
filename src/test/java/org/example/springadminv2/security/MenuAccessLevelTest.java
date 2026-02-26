package org.example.springadminv2.security;

import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuAccessLevelTest {

    @Test
    @DisplayName("코드 'R'로 READ 레벨을 조회한다")
    void from_code_read() {
        // when & then
        assertThat(MenuAccessLevel.fromCode("R")).isEqualTo(MenuAccessLevel.READ);
    }

    @Test
    @DisplayName("코드 'W'로 WRITE 레벨을 조회한다")
    void from_code_write() {
        // when & then
        assertThat(MenuAccessLevel.fromCode("W")).isEqualTo(MenuAccessLevel.WRITE);
    }

    @Test
    @DisplayName("알 수 없는 코드는 IllegalArgumentException을 던진다")
    void from_code_unknown_throws() {
        // when & then
        assertThatThrownBy(() -> MenuAccessLevel.fromCode("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown");
    }
}
