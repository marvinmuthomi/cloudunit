package fr.treeptik.cloudunit.dto;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.ResourceSupport;

import fr.treeptik.cloudunit.model.Port;

public class ModulePortResource extends ResourceSupport {
    private String number;
    
    private String hostNumber;
    
    @NotNull
	private Boolean publishPort;
	
	/**
	 * @deprecated only to be used by Jackson deserialization.
	 */
	@Deprecated
	protected ModulePortResource() {}
	
	private ModulePortResource(Builder builder) {
		this.publishPort = builder.publishPort;
	}

	public ModulePortResource(Port port) {
        this.publishPort = port.isOpen();
        this.number = port.getContainerValue();
        this.hostNumber = port.getHostValue();
    }

    public static class Builder {

		private Boolean publishPort;

		public Builder withPublishPort(Boolean publishPort) {
			this.publishPort = publishPort;
			return this;
		}

		public ModulePortResource build() {
			return new ModulePortResource(this);
		}
	}

	public Boolean getPublishPort() {
		return publishPort;
	}
	
	public void setPublishPort(Boolean publishPort) {
        this.publishPort = publishPort;
    }
	
	public String getNumber() {
        return number;
    }
	
	public void setNumber(String number) {
        this.number = number;
    }
	
	public String getHostNumber() {
        return hostNumber;
    }
	
	public void setHostNumber(String hostNumber) {
        this.hostNumber = hostNumber;
    }

    public static Builder of() {
        return new Builder();
    }
}
