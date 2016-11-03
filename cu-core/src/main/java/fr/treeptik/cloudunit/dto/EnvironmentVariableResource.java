package fr.treeptik.cloudunit.dto;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.hateoas.ResourceSupport;

import fr.treeptik.cloudunit.model.EnvironmentVariable;

public class EnvironmentVariableResource extends ResourceSupport {
    @NotNull
    @Pattern(regexp = "[_A-Z][_A-Z0-9]+")
    private String name;
    
    @NotNull
    private String value;

    public EnvironmentVariableResource() {}
    
    public EnvironmentVariableResource(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public EnvironmentVariableResource(EnvironmentVariable environmentVariable) {
        this.name = environmentVariable.getKeyEnv();
        this.value = environmentVariable.getValueEnv();
    }
    
    public static List<EnvironmentVariableResource> fromDefinitions(String definitions) {
        return Arrays.stream(definitions.split("\n"))
                .map(s -> s.trim())
                .filter(s -> !StringUtils.isBlank(s))
                .map(s -> fromDefinition(s))
                .collect(Collectors.toList());
    }
    
    public static EnvironmentVariableResource fromDefinition(String definition) {
        Matcher matcher = java.util.regex.Pattern.compile("([_A-Z][_A-Z0-9]*)=(.*)").matcher(definition);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Could not parse variable definition: "+definition);
        }

        return new EnvironmentVariableResource(matcher.group(1), matcher.group(2));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof EnvironmentVariableResource)) return false;
        
        EnvironmentVariableResource other = (EnvironmentVariableResource) obj;
        return new EqualsBuilder()
                .append(this.name, other.name)
                .append(this.value, other.value)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(name)
                .append(value)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("value", value)
                .appendSuper(super.toString())
                .toString();
    }
}
