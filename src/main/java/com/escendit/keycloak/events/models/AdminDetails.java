package com.escendit.keycloak.events.models;

import javax.annotation.Nullable;

public record AdminDetails(
        @Nullable
        String realmId,
        @Nullable
        String clientId,
        @Nullable
        String userId,
        @Nullable
        String ipAddress) {
}
