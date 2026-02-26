package org.example.springadminv2.global.security.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

        Set<String> authorities = new LinkedHashSet<>();
        authorities.addAll(splitAuthorities(entry.getOrDefault("R", "")));
        if (level == MenuAccessLevel.WRITE) {
            authorities.addAll(splitAuthorities(entry.getOrDefault("W", "")));
        }

        return authorities;
    }

    private Set<String> splitAuthorities(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
