package fr.treeptik.cloudunit.model;/*
									* LICENCE : CloudUnit is available under the GNU Affero General Public License : https://gnu.org/licenses/agpl.html
									* but CloudUnit is licensed too under a standard commercial license.
									* Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
									* If you are not sure whether the AGPL is right for you,
									* you can always test our software under the AGPL and inspect the source code before you contact us
									* about purchasing a commercial license.
									*
									* LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
									* or promote products derived from this project without prior written permission from Treeptik.
									* Products or services derived from this software may not be called "CloudUnit"
									* nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
									* For any questions, contact us : contact@treeptik.fr
									*/

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class Deployment {
	@Id
	@GeneratedValue
	private Integer id;
	
	@ManyToOne
	private Application application;

	@Temporal(TemporalType.TIMESTAMP)
	private Date date;
	
	private String contextPath;
	
	@Enumerated(EnumType.STRING)
	private DeploymentType type;
	
	protected Deployment() {}
	
	public Deployment(Application application, String contextPath, DeploymentType type) {
	    this.application = application;
	    this.contextPath = contextPath;
	    this.type = type;
	    this.date = new Date();
	}

	public Integer getId() {
		return id;
	}
	
	public Application getApplication() {
        return application;
    }

	public Date getDate() {
		return date;
	}

	public DeploymentType getType() {
		return type;
	}

	public void setType(DeploymentType type) {
		this.type = type;
	}
	
	public String getContextPath() {
        return contextPath;
    }
    
    public String getUri() {
        return application.getLocation() + (contextPath.equals("ROOT") ? "/" : contextPath);
    }
}
