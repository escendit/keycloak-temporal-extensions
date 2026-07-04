package com.escendit.keycloak.events.workflows;

import com.escendit.keycloak.events.models.AdminEvent;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AdminEventWorkflow {
    @WorkflowMethod(name = "IdentityAdminEvent")
    void send(AdminEvent event);
}
