package fr.treeptik.cloudunit.dto;

import fr.treeptik.cloudunit.enums.JavaRelease;
import fr.treeptik.cloudunit.enums.JvmMemory;

//TODO validation see CheckUtils.checkJavaOpts
public class JvmConfigurationResource {
    private JvmMemory memory;
    private String options;
    private JavaRelease release;
    
    public JvmConfigurationResource() {}

    public JvmMemory getMemory() {
        return memory;
    }

    public void setMemory(JvmMemory memory) {
        this.memory = memory;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public JavaRelease getRelease() {
        return release;
    }

    public void setRelease(JavaRelease release) {
        this.release = release;
    }
}
