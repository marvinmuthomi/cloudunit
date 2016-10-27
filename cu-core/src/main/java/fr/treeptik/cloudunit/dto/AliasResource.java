package fr.treeptik.cloudunit.dto;

import javax.validation.constraints.Size;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AliasResource extends ResourceSupport {
    @Size(max = 64)
    private final String name;
    
    @JsonCreator
    public AliasResource(@JsonProperty("name") String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
