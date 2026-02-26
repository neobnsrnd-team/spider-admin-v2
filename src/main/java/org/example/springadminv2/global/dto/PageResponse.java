package org.example.springadminv2.global.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int page, int size, int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PageResponse<>(content, totalElements, page, size, totalPages);
    }
}
