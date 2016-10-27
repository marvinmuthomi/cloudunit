package fr.treeptik.cloudunit.dto;

import java.util.Arrays;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.treeptik.cloudunit.enums.JavaRelease;
import fr.treeptik.cloudunit.enums.JvmMemory;
import fr.treeptik.cloudunit.model.Server;

public class ServerResource extends ResourceSupport {
    @NotNull(groups = Full.class)
    private Long jvmMemory;
    
    @NotNull(groups = Full.class)
    private String jvmRelease;
    
    @Pattern(regexp = "^[^-]*(-(?!Xms)[^-]*)*$", groups = Patch.class)
    private String jvmOptions;

    public ServerResource() {}
    
    public ServerResource(Server server) {
        this.jvmMemory = server.getJvmMemory();
        this.jvmRelease = server.getJvmRelease();
        this.jvmOptions = server.getJvmOptions();
    }

    public Long getJvmMemory() {
        return jvmMemory;
    }

    public void setJvmMemory(Long jvmMemory) {
        this.jvmMemory = jvmMemory;
    }

    public String getJvmRelease() {
        return jvmRelease;
    }

    public void setJvmRelease(String jvmRelease) {
        this.jvmRelease = jvmRelease;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public void patch(Server server) {
        if (jvmMemory != null) {
            server.setJvmMemory(jvmMemory);
        }
        
        if (jvmOptions != null) {
            server.setJvmOptions(jvmOptions);
        }
        
        if (jvmRelease != null) {
            server.setJvmRelease(jvmRelease);
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return jvmMemory == null && jvmOptions == null && jvmRelease == null;
    }
    
    @AssertTrue(message = "Invalid memory size", groups = Patch.class)
    @JsonIgnore
    public boolean isValidMemory() {
        return jvmMemory == null
                || Arrays.stream(JvmMemory.values())
                    .filter(v -> v.getSize().equals(jvmMemory.toString()))
                    .findAny()
                    .isPresent();
    }
    
    @AssertTrue(message = "Invalid Java release", groups = Patch.class)
    @JsonIgnore
    public boolean isValidRelease() {
        return jvmRelease == null
                || Arrays.stream(JavaRelease.values())
                    .filter(r -> r.getVersion().equals(jvmRelease))
                    .findAny()
                    .isPresent();
    }

    public void put(Server server) {
        server.setJvmMemory(jvmMemory);
        server.setJvmOptions(jvmOptions);
        server.setJvmRelease(jvmRelease);
    }
    
    /**
     * Bean validation group for full request validation.
     */
    public interface Full extends Patch {}
    
    /**
     * Bean validation group for validating only specified fields.
     */
    public interface Patch {}
}
