package com.escendit.keycloak.events.providers;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import com.escendit.keycloak.events.models.AdminDetails;
import com.escendit.keycloak.events.models.UserEvent;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import com.escendit.keycloak.events.workflows.AdminEventWorkflow;
import com.escendit.keycloak.events.workflows.UserEventWorkflow;

public class TemporalioEventListenerProvider implements EventListenerProvider {
    private final Logger _logger = Logger.getLogger(TemporalioEventListenerProvider.class);
    private final WorkflowClient _workflowClient;
    private final String _userTaskQueue;
    private final String _adminTaskQueue;

    public TemporalioEventListenerProvider(WorkflowClient workflowClient, String userTaskQueue, String adminTaskQueue) {
        if (workflowClient == null) {
            throw new IllegalArgumentException("workflowClient cannot be null");
        }
        if (userTaskQueue == null) {
            throw new IllegalArgumentException("userTaskQueue cannot be null");
        }
        if (adminTaskQueue == null) {
            throw new IllegalArgumentException("adminTaskQueue cannot be null");
        }

        _workflowClient = workflowClient;
        _userTaskQueue = userTaskQueue;
        _adminTaskQueue = adminTaskQueue;
    }

    @Override
    public void onEvent(Event event) {
        if (event == null) {
            return;
        }

        if (_logger.isDebugEnabled()) {
            _logger.debugf("onEvent(id=%s,time=%d,type=%s)", event.getId(), event.getTime(), event.getType());
        }

        final var eventParameter = new UserEvent(
                event.getId(),
                event.getTime(),
                event.getType().toString(),
                event.getRealmId(),
                event.getClientId(),
                event.getUserId(),
                event.getSessionId(),
                event.getIpAddress(),
                event.getError(),
                event.getDetails()
        );

        final var userEventWorkflowStub = _workflowClient
                .newWorkflowStub(
                        UserEventWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(_userTaskQueue)
                                .setWorkflowId(event.getId())
                                .build());

        try {
            if (_logger.isDebugEnabled()) {
                _logger.debugf("Starting user workflow %s", userEventWorkflowStub);
            }

            userEventWorkflowStub.send(eventParameter);

            if (_logger.isDebugEnabled()) {
                _logger.debugf("Started user workflow (workflowId=%s)", event.getId());
            }
        } catch (Exception e) {
            _logger.errorf(e, "Error while starting user workflow %s", userEventWorkflowStub);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event == null) {
            return;
        }

        if (_logger.isDebugEnabled()) {
            _logger.debugf(
                    "onAdminEvent(id=%s,time=%d,resourceType=%s,operationType=%s)",
                    event.getId(),
                    event.getTime(),
                    event.getResourceTypeAsString(),
                    event.getOperationType().name());
        }

        AdminDetails authDetails = null;
        String representation = null;

        if (event.getAuthDetails() != null) {
            var details = event.getAuthDetails();
            authDetails = new AdminDetails(
                    details.getRealmId(),
                    details.getClientId(),
                    details.getUserId(),
                    details.getIpAddress());
        }

        if (includeRepresentation) {
            representation = event.getRepresentation();
        }

        var eventParameter = new com.escendit.keycloak.events.models.AdminEvent(
                event.getId(),
                event.getTime(),
                event.getRealmId(),
                authDetails,
                event.getResourceTypeAsString(),
                event.getOperationType().toString(),
                event.getResourcePath(),
                representation,
                event.getError()
        );

        final var adminEventWorkflowStub = _workflowClient
                .newWorkflowStub(
                        AdminEventWorkflow.class,
                        WorkflowOptions
                                .newBuilder()
                                .setTaskQueue(_adminTaskQueue)
                                .setWorkflowId(event.getId())
                                .build());

        try {
            if (_logger.isDebugEnabled()) {
                _logger.debugf("Starting admin workflow %s", adminEventWorkflowStub);
            }

            adminEventWorkflowStub.send(eventParameter);

            if (_logger.isDebugEnabled()) {
                _logger.debugf("Started admin workflow (workflowId=%s)", event.getId());
            }
        } catch (Exception e) {
            _logger.errorf("Error starting admin workflow %s", adminEventWorkflowStub, e);
        }
    }

    @Override
    public void close() {
        if (_logger.isDebugEnabled()) {
            _logger.debugf("Closing %s", getClass().getName());
        }
    }
}
