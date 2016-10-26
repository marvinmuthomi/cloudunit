package fr.treeptik.cloudunit.dto;

import org.springframework.hateoas.ResourceSupport;

import fr.treeptik.cloudunit.model.Server;

public class ServerResource extends ResourceSupport {
    
    public ServerResource() {}
    
    public ServerResource(Server server) {
        
    }
}
