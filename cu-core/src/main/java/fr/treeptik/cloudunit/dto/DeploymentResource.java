package fr.treeptik.cloudunit.dto;

import org.springframework.hateoas.ResourceSupport;

import fr.treeptik.cloudunit.model.Deployment;
import fr.treeptik.cloudunit.model.DeploymentType;

public class DeploymentResource extends ResourceSupport {
    private String contextPath;
    private DeploymentType type;

    public DeploymentResource() {}
    
    public DeploymentResource(Deployment deployment) {
        this.contextPath = deployment.getContextPath();
        this.type = deployment.getType();
    }
    
    public String getContextPath() {
        return contextPath;
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public DeploymentType getType() {
        return type;
    }
    
    public void setType(DeploymentType type) {
        this.type = type;
    }
}
