package org.example.springadminv2.domain.wasproperty.dto;

public record WasPropertyHistoryVersionResponse(
        int version, String reason, String lastUpdateUserId, String lastUpdateDtime) {}
