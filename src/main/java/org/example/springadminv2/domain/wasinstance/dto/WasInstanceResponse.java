package org.example.springadminv2.domain.wasinstance.dto;

public record WasInstanceResponse(
        String instanceId, String instanceName, String instanceDesc, String ip, String port) {}
