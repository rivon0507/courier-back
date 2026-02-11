package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.web.error.ApiException;
import org.springframework.http.HttpStatus;

public class WorkspaceNotFoundException extends ApiException {
    public WorkspaceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "Workspace not found");
    }
}
