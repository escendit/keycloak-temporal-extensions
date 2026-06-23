package com.escendit.keycloak.events.providers;

import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TemporalioEventListenerProviderFactory implements EventListenerProviderFactory {
    private final Logger _logger = Logger.getLogger(TemporalioEventListenerProviderFactory.class);
    private WorkflowClient _workflowClient = null;
    private WorkflowServiceStubs _workflowServiceStubs = null;
    private String _targetHost = "localhost:7233";
    private String _namespace = "default";
    private String _adminTaskQueue = "keycloak-admin-queue";
    private String _userTaskQueue = "keycloak-user-queue";
    private String _tlsCert = null;
    private String _tlsKey = null;
    private String _channelOverrideAuthority = null;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        if (_logger.isDebugEnabled()) {
            _logger.debugf("Creating %s", TemporalioEventListenerProvider.class);
        }
        return new TemporalioEventListenerProvider(_workflowClient, _userTaskQueue, _adminTaskQueue);
    }

    @Override
    public void init(Config.Scope config) {
        if (_logger.isDebugEnabled()) {
            _logger.debugf("Initializing %s", TemporalioEventListenerProviderFactory.class);
        }

        /* Apply Target Host if specified, otherwise use the default */
        var targetHostConfig = config.get("target-host");

        if (targetHostConfig != null && !targetHostConfig.isEmpty()) {
            _targetHost = targetHostConfig;
        }

        /* Apply Namespace if specified, otherwise use the default */
        var namespaceConfig = config.get("namespace");

        if (namespaceConfig != null && !namespaceConfig.isEmpty()) {
            _namespace = namespaceConfig;
        }

        /* Apply Admin and User Task Queues if specified, otherwise use the default */
        var adminTaskQueueConfig = config.get("admin-task-queue");

        if (adminTaskQueueConfig != null && !adminTaskQueueConfig.isEmpty()) {
            _adminTaskQueue = adminTaskQueueConfig;
        }

        var userTaskQueueConfig = config.get("user-task-queue");

        if (userTaskQueueConfig != null && !userTaskQueueConfig.isEmpty()) {
            _userTaskQueue = userTaskQueueConfig;
        }

        /* Apply TLS Cert and Key if specified, otherwise use the default */
        var tlsCertConfig = config.get("mtls-crt-file");
        var tlsKeyConfig = config.get("mtls-key-file");

        if ((tlsCertConfig != null && tlsKeyConfig != null) && !(tlsCertConfig.isBlank() || tlsKeyConfig.isBlank())) {
            _tlsCert = tlsCertConfig;
            _tlsKey = tlsKeyConfig;
        }

        /* Apply Channel Override Authority if specified, otherwise use the default */
        var channelOverrideAuthorityConfig = config.get("mtls-override-authority");

        if (channelOverrideAuthorityConfig != null && !channelOverrideAuthorityConfig.isEmpty()) {
            _channelOverrideAuthority = channelOverrideAuthorityConfig;
        }

        var workflowServiceStubsOptionsBuilder = WorkflowServiceStubsOptions
                .newBuilder()
                .setTarget(_targetHost);

        if (_tlsCert != null && _tlsKey != null && !_tlsCert.isBlank() && !_tlsKey.isBlank()) {
            try (FileInputStream certInputStream = new FileInputStream(_tlsCert);
                 FileInputStream keyInputStream = new FileInputStream(_tlsKey)) {
                SslContext sslContext = SimpleSslContextBuilder
                        .forPKCS8(certInputStream, keyInputStream)
                        .build();

                workflowServiceStubsOptionsBuilder.setSslContext(sslContext);

                if (_channelOverrideAuthority != null && !_channelOverrideAuthority.isBlank()) {
                    workflowServiceStubsOptionsBuilder
                            .setChannelInitializer(channel -> channel.overrideAuthority(_channelOverrideAuthority));
                }
            } catch (FileNotFoundException e) {
                _logger.errorf(e, "File not found: %s", e.getMessage());
            } catch (IOException e) {
                _logger.errorf(e, "Error while reading TLS cert and key files: %s", e.getMessage());
            }
        }

        _workflowServiceStubs = WorkflowServiceStubs
                .newServiceStubs(workflowServiceStubsOptionsBuilder.build());

        _workflowClient = WorkflowClient
                .newInstance(_workflowServiceStubs, WorkflowClientOptions
                        .newBuilder()
                        .setNamespace(_namespace)
                        .build());
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (_logger.isDebugEnabled()) {
            _logger.debugf("Global Initialization (%s)", TemporalioEventListenerProviderFactory.class);
        }
    }

    @Override
    public void close() {
        _workflowClient = null;
        if (!_workflowServiceStubs.isShutdown()) {
            _workflowServiceStubs.shutdown();
            _workflowServiceStubs.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    @Override
    public String getId() {
        return "temporal";
    }
}
