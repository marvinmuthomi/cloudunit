package fr.treeptik.cloudunit.dto;

import java.util.Date;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Status;

public class ApplicationResource extends ResourceSupport {
    @Pattern(regexp = "^[a-z][a-z0-9-]*[a-z0-9]$")
    private String name;

    @Pattern(regexp = ".*\\S.*")
    private String displayName;
    
    private String domainName;
    
    private Status status;
    
    private String deploymentStatus;
    
    private String userDisplayName;
    
    @NotNull
    @Pattern(regexp = "^[a-z][a-z0-9-]*[a-z0-9]$")
    private String serverType;
    
    @JsonFormat(pattern = "YYYY-MM-dd HH:mm")
    private Date creationDate;
    
    public ApplicationResource() {}
    
    public ApplicationResource(Application application) {
        this.name = application.getName();
        this.displayName = application.getDisplayName();
        this.domainName = application.getDomainName();
        this.status = application.getStatus();
        this.deploymentStatus = application.getDeploymentStatus();
        this.creationDate = application.getDate();
        
        this.userDisplayName = application.getUser().getDisplayName();
        this.serverType = application.getServer().getImage().getName();
    }

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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDeploymentStatus() {
        return deploymentStatus;
    }

    public void setDeploymentStatus(String deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    
    public String getUserDisplayName() {
        return userDisplayName;
    }
    
    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }
    
    public String getServerType() {
        return serverType;
    }
    
    public void setServerType(String serverType) {
        this.serverType = serverType;
    }
    
    @JsonIgnore
    @AssertTrue(message = "One of name or displayName must be suppied")
    public boolean isNameOrDisplayNameGiven() {
        return displayName != null || name != null;
    }
}
