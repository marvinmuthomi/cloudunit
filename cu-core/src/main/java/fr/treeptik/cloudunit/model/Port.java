package fr.treeptik.cloudunit.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import fr.treeptik.cloudunit.enums.PortType;

/**
 * Created by guillaume on 25/09/16.
 */
@Entity
public class Port implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private PortType portType;

    private String hostValue;

    private String containerValue;

    private boolean open;

    public Port() {}

    public Port(PortType portType, String containerValue, String hostValue, boolean open) {
        this.portType = portType;
        this.hostValue = hostValue;
        this.containerValue = containerValue;
        this.open = open;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public PortType getPortType() {
        return portType;
    }

    public void setPortType(PortType portType) {
        this.portType = portType;
    }

    public String getHostValue() {
        return hostValue;
    }

    public void setHostValue(String hostValue) {
        this.hostValue = hostValue;
    }

    public String getContainerValue() {
        return containerValue;
    }

    public void setContainerValue(String containerValue) {
        this.containerValue = containerValue;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
