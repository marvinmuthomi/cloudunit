package fr.treeptik.cloudunit.cli.model;

import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.ServerResource;

public class ServerAndContainer {
    public final ServerResource server;
    public final ContainerResource container;
    
    public ServerAndContainer(ServerResource server, ContainerResource container) {
        this.server = server;
        this.container = container;
    }
}
