package org.example.springadminv2.domain.property.dto;

public record PropertyHistoryResponse(
        String propertyId,
        String propertyName,
        String propertyDesc,
        String dataType,
        String validData,
        String defaultValue) {}
