package fr.treeptik.cloudunit.dto;

import org.springframework.hateoas.ResourceSupport;

import fr.treeptik.cloudunit.model.Image;

public class ImageResource extends ResourceSupport {
    private String name;
    private String displayName;

    public ImageResource() {}
    
    public ImageResource(Image image) {
        this.name = image.getName();
        this.displayName = image.getDisplayName();
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
    
}
