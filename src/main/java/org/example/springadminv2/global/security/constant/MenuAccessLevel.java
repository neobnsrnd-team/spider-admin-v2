package org.example.springadminv2.global.security.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MenuAccessLevel {
    READ("R"),
    WRITE("W");

    private final String code;

    public static MenuAccessLevel fromCode(String code) {
        for (MenuAccessLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown MenuAccessLevel code: " + code);
    }
}
