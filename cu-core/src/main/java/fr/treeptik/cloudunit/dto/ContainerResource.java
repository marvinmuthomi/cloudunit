package fr.treeptik.cloudunit.dto;

import java.util.Date;

import org.springframework.hateoas.ResourceSupport;

import fr.treeptik.cloudunit.model.Container;
import fr.treeptik.cloudunit.model.Status;

public class ContainerResource extends ResourceSupport {
    private String shortId;
    private String fullId;
    private String name;
    private Date startDate;
    private String dockerState;
    private Status status;
    private Long memorySize;
    private String ipAddress;
    private String internalDnsName;
    private String sshPort;

    public ContainerResource() {}
    
    public ContainerResource(Container container) {
        this.shortId = container.getContainerID();
        this.fullId = container.getContainerFullID();
        this.name = container.getName();
        this.startDate = container.getStartDate();
        this.dockerState = container.getDockerState();
        this.status = container.getStatus();
        this.memorySize = container.getMemorySize();
        this.ipAddress = container.getContainerIP();
        this.internalDnsName = container.getInternalDNSName();
        this.sshPort = container.getSshPort();
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public String getFullId() {
        return fullId;
    }

    public void setFullId(String fullId) {
        this.fullId = fullId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getDockerState() {
        return dockerState;
    }

    public void setDockerState(String dockerState) {
        this.dockerState = dockerState;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getInternalDnsName() {
        return internalDnsName;
    }

    public void setInternalDnsName(String internalDnsName) {
        this.internalDnsName = internalDnsName;
    }

    public String getSshPort() {
        return sshPort;
    }

    public void setSshPort(String sshPort) {
        this.sshPort = sshPort;
    }
}
