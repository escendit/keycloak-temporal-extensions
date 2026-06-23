package com.escendit.keycloak.events.models;

import javax.annotation.Nullable;

public record AdminEvent(
        @Nullable
        String id,
        @Nullable
        Long time,
        @Nullable
        String realmId,
        @Nullable
        AdminDetails authDetails,
        @Nullable
        String resourceType,
        @Nullable
        String operationType,
        @Nullable
        String resourcePath,
        @Nullable
        String representation,
        @Nullable
        String error
) {
}
