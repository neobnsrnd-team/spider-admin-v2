package org.example.springadminv2.global.security.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;

@Component
public class MenuResourcePermissions {

    private static final String YAML_PATH = "menu-resource-permissions.yml";
    private static final String READ_SUFFIX = "_READ";
    private static final String WRITE_SUFFIX = "_WRITE";
    private static final Set<String> SPECIAL_VALUES = Set.of("permitAll", "isAuthenticated");

    private final Map<String, List<String>> readResources = new HashMap<>();
    private final Map<String, List<String>> writeResources = new HashMap<>();

    @PostConstruct
    void init() {
        Yaml yaml = new Yaml();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(YAML_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + YAML_PATH);
            }
            Map<String, String> entries = yaml.load(is);
            if (entries == null) {
                return;
            }
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (SPECIAL_VALUES.contains(value)) {
                    continue;
                }

                if (key.endsWith(READ_SUFFIX)) {
                    String menuId = key.substring(0, key.length() - READ_SUFFIX.length());
                    readResources.put(menuId, parseResources(value));
                } else if (key.endsWith(WRITE_SUFFIX)) {
                    String menuId = key.substring(0, key.length() - WRITE_SUFFIX.length());
                    writeResources.put(menuId, parseResources(value));
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to load " + YAML_PATH, e);
        }
    }

    public Set<String> getDerivedResourceAuthorities(String menuId, MenuAccessLevel level) {
        Set<String> authorities = new HashSet<>();

        List<String> reads = readResources.getOrDefault(menuId, Collections.emptyList());
        authorities.addAll(reads);

        if (level == MenuAccessLevel.WRITE) {
            List<String> writes = writeResources.getOrDefault(menuId, Collections.emptyList());
            authorities.addAll(writes);
        }

        return authorities;
    }

    private List<String> parseResources(String value) {
        List<String> resources = new ArrayList<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                resources.add(toAuthorityFormat(trimmed));
            }
        }
        return resources;
    }

    private String toAuthorityFormat(String yamlResource) {
        if (yamlResource.endsWith(READ_SUFFIX)) {
            return yamlResource.substring(0, yamlResource.length() - READ_SUFFIX.length())
                    + ":"
                    + MenuAccessLevel.READ.getCode();
        }
        if (yamlResource.endsWith(WRITE_SUFFIX)) {
            return yamlResource.substring(0, yamlResource.length() - WRITE_SUFFIX.length())
                    + ":"
                    + MenuAccessLevel.WRITE.getCode();
        }
        return yamlResource;
    }
}
