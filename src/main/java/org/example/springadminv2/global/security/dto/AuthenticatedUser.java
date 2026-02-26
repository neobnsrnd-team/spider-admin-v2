package org.example.springadminv2.global.security.dto;

public record AuthenticatedUser(
        String userId, String password, String roleId, String userStateCode, int loginFailCount) {}
