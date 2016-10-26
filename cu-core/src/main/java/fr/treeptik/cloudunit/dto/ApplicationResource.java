package fr.treeptik.cloudunit.dto;

import java.util.Date;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonFormat;

import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Status;

public class ApplicationResource extends ResourceSupport {
    private String name;
    private String displayName;
    private String domainName;
    private Status status;
    private String deploymentStatus;
    
    @JsonFormat(pattern = "YYYY-MM-dd HH:mm")
    private Date creationDate;
    
    public ApplicationResource() {}
    
    public ApplicationResource(Application application) {
        this.name = application.getName();
        this.displayName = application.getDisplayName();
        this.domainName = application.getDomainName();
        this.status = application.getStatus();
        this.deploymentStatus = application.getDeploymentStatus();
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
}
