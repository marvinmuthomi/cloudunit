package fr.treeptik.cloudunit.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.hateoas.ResourceSupport;

import fr.treeptik.cloudunit.model.Module;

public class ModuleResource extends ResourceSupport {
    @NotNull
    @Pattern(regexp = "[a-z][a-z0-9-]*")
    @Size(max = 64)
    private String name;
    private String displayName;
    
    public ModuleResource() {}
    
    public ModuleResource(Module module) {
        this.name = module.getImage().getName();
        this.displayName = module.getImage().getDisplayName();
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
