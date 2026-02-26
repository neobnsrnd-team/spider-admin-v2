package org.example.springadminv2.domain.property.dto;

public record PropertyGroupSearchRequest(
        String searchType, String keyword, int page, int size, String sortBy, String sortDirection) {

    public PropertyGroupSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (sortBy == null || sortBy.isBlank()) sortBy = "PROPERTY_GROUP_ID";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "ASC";
    }

    public int offset() {
        return page * size;
    }
}
