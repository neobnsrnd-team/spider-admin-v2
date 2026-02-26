package org.example.springadminv2.domain.property.dto;

public record PropertyHistoryVersionResponse(
        int version, String reason, String lastUpdateUserId, String lastUpdateDtime) {}
