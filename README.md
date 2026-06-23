# Keycloak Temporal Extensions

A Keycloak SPI Event Listener that publishes user and admin events to a [Temporal](https://temporal.io) service or Temporal Cloud.

## Overview

This extension registers itself as a Keycloak `EventListenerProvider` under the ID `temporal`. When enabled, it forwards Keycloak events to Temporal workflows via two separate task queues:

- **User events** — login, logout, registration, and other user-facing events → `UserEventWorkflow` (`IdentityUserEvent`)
- **Admin events** — realm management, user management, and other admin console actions → `AdminEventWorkflow` (`IdentityAdminEvent`)

## Requirements

- Java 25+
- Keycloak 26.6.x
- Temporal SDK 1.35.0
- A running Temporal service or Temporal Cloud account

## Building

```bash
mvn package
```

The resulting JAR will be in `target/listener-temporal-<version>.jar`.

## Installation

Copy the JAR into your Keycloak `providers/` directory and restart Keycloak (or run `kc.sh build` if using Keycloak's quarkus distribution).

## Configuration

Configure the provider via Keycloak's SPI configuration. All settings are optional and fall back to defaults.

| Key | Default | Description |
|-----|---------|-------------|
| `target-host` | `localhost:7233` | Temporal service gRPC endpoint |
| `namespace` | `default` | Temporal namespace |
| `user-task-queue` | `keycloak-user-queue` | Task queue for user events |
| `admin-task-queue` | `keycloak-admin-queue` | Task queue for admin events |
| `mtls-crt-file` | — | Path to mTLS client certificate (PEM) |
| `mtls-key-file` | — | Path to mTLS private key (PKCS8 PEM) |
| `mtls-override-authority` | — | gRPC channel authority override (for Temporal Cloud) |

### Example: `keycloak.conf`

```properties
spi-events-listener-temporal-target-host=temporal.example.com:7233
spi-events-listener-temporal-namespace=my-namespace
spi-events-listener-temporal-user-task-queue=my-user-queue
spi-events-listener-temporal-admin-task-queue=my-admin-queue
```

### Temporal Cloud (mTLS)

```properties
spi-events-listener-temporal-target-host=<account>.tmprl.cloud:7233
spi-events-listener-temporal-namespace=<namespace>.<account>
spi-events-listener-temporal-mtls-crt-file=/path/to/client.pem
spi-events-listener-temporal-mtls-key-file=/path/to/client.key
spi-events-listener-temporal-mtls-override-authority=<namespace>.<account>.tmprl.cloud
```

## Enabling the Listener

In the Keycloak Admin Console, navigate to **Realm Settings → Events → Event listeners** and add `temporal` to the list.

## Workflow Contracts

Your Temporal workers must implement the following workflow interfaces:

### `UserEventWorkflow`

```java
@WorkflowInterface
public interface UserEventWorkflow {
    @WorkflowMethod(name = "IdentityUserEvent")
    void send(UserEvent event);
}
```

**`UserEvent` fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Event ID |
| `time` | `Long` | Epoch milliseconds |
| `type` | `String` | Event type (e.g. `LOGIN`, `LOGOUT`) |
| `realmId` | `String` | Realm ID |
| `clientId` | `String` | Client ID |
| `userId` | `String` | User ID |
| `sessionId` | `String` | Session ID |
| `ipAddress` | `String` | Client IP |
| `error` | `String` | Error message, if any |
| `details` | `Map<String, String>` | Additional event details |

### `AdminEventWorkflow`

```java
@WorkflowInterface
public interface AdminEventWorkflow {
    @WorkflowMethod(name = "IdentityAdminEvent")
    void send(AdminEvent event);
}
```

**`AdminEvent` fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Event ID |
| `time` | `Long` | Epoch milliseconds |
| `realmId` | `String` | Realm ID |
| `authDetails` | `AdminDetails` | Auth context (realm, client, user, IP) |
| `resourceType` | `String` | Resource type (e.g. `USER`, `CLIENT`) |
| `operationType` | `String` | Operation (e.g. `CREATE`, `UPDATE`, `DELETE`) |
| `resourcePath` | `String` | Path to the affected resource |
| `representation` | `String` | JSON representation of the resource (when included) |
| `error` | `String` | Error message, if any |

## License

Licensed under the [Apache License 2.0](LICENSE).

Copyright (c) Escendit — [https://github.com/escendit](https://github.com/escendit)
