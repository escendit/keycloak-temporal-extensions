package com.escendit.keycloak.events.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import com.escendit.keycloak.events.models.AdminEvent;

@WorkflowInterface
public interface AdminEventWorkflow {
    @WorkflowMethod(name = "IdentityAdminEvent")
    void send(AdminEvent event);
}
