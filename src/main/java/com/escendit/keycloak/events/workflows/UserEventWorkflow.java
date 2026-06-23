package com.escendit.keycloak.events.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import com.escendit.keycloak.events.models.UserEvent;

@WorkflowInterface()
public interface UserEventWorkflow {
    @WorkflowMethod(name = "IdentityUserEvent")
    void send(UserEvent event);
}
