package com.escendit.keycloak.events.models;

import javax.annotation.Nullable;
import java.util.Map;

public record UserEvent(
        @Nullable
        String id,
        @Nullable
        Long time,
        @Nullable
        String type,
        @Nullable
        String realmId,
        @Nullable
        String clientId,
        @Nullable
        String userId,
        @Nullable
        String sessionId,
        @Nullable
        String ipAddress,
        @Nullable
        String error,
        @Nullable
        Map<String, String> details) {
}
