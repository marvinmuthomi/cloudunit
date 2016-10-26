package fr.treeptik.cloudunit.dto;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class ApplicationCreationRequest {
    @Pattern(regexp = "^[a-z][a-z0-9-]*[a-z0-9]$")
    private String name;
    
    @Pattern(regexp = ".*\\S.*")
    private String displayName;
    
    @NotNull
    @Pattern(regexp = "^[a-z][a-z0-9-]*[a-z0-9]$")
    private String serverType;
    
    public ApplicationCreationRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }
    
    @AssertTrue(message = "One of name or displayName must be suppied")
    public boolean isNameOrDisplayNameGiven() {
        return displayName != null || name != null;
    }
}
