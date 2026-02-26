package org.example.springadminv2.global.security.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "menu-resource")
public class MenuResourcePermissions {

    private Map<String, Map<String, String>> permissions = Collections.emptyMap();

    public Map<String, Map<String, String>> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Map<String, String>> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getDerivedResourceAuthorities(String menuId, MenuAccessLevel level) {
        Map<String, String> entry = permissions.getOrDefault(menuId, Collections.emptyMap());

        String readValue = entry.getOrDefault("R", "");
        String writeValue = level == MenuAccessLevel.WRITE ? entry.getOrDefault("W", "") : "";

        String combined = readValue + "," + writeValue;

        return Arrays.stream(combined.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
