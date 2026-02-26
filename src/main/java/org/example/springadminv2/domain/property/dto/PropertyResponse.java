package org.example.springadminv2.domain.property.dto;

public record PropertyResponse(
        String groupId,
        String propertyId,
        String propertyName,
        String propertyDesc,
        String dataType,
        String validData,
        String defaultValue,
        String lastUpdateUserId,
        String lastUpdateDtime) {}
