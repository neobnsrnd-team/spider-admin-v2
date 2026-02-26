package org.example.springadminv2.domain.property.dto;

import jakarta.validation.constraints.NotBlank;

public record PropertyItemRequest(
        @NotBlank String propertyId,
        @NotBlank String propertyName,
        @NotBlank String propertyDesc,
        String dataType,
        String validData,
        String defaultValue,
        String useYn,
        String crud) {

    public PropertyItemRequest {
        if (useYn == null) useYn = "Y";
    }
}
