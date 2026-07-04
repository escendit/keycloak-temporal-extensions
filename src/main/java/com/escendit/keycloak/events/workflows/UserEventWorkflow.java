package com.escendit.keycloak.events.workflows;

import com.escendit.keycloak.events.models.UserEvent;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface()
public interface UserEventWorkflow {
    @WorkflowMethod(name = "IdentityUserEvent")
    void send(UserEvent event);
}
