package fr.treeptik.cloudunit.cli.model;

import java.util.Collection;

import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.ModulePortResource;
import fr.treeptik.cloudunit.dto.ModuleResource;

public class ModuleAndContainer {
    public final ModuleResource module;
    public final ContainerResource container;
    public final Collection<ModulePortResource> ports;
    
    public ModuleAndContainer(ModuleResource module, ContainerResource container, Collection<ModulePortResource> ports) {
        this.module = module;
        this.container = container;
        this.ports = ports;
    }
}
