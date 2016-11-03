/*
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

package fr.treeptik.cloudunit.dto;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.treeptik.cloudunit.model.PortToOpen;

/**
 * Created by nicolas on 31/07/2014.
 */
public class PortResource extends ResourceSupport {
    @Min(1)
    @NotNull
	private Integer number;

    @NotNull
	private String nature;
	
	private Boolean quickAccess;

	public PortResource() {}

	public PortResource(PortToOpen portToOpen) {
		this.nature = portToOpen.getNature();
		this.quickAccess = portToOpen.getQuickAccess();
		this.number = portToOpen.getPort();
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public String getNature() {
		return nature;
	}

	public void setNature(String nature) {
		this.nature = nature;
	}
	
	public Boolean isQuickAccess() {
		return quickAccess;
	}

	public void setQuickAccess(Boolean quickAccess) {
		this.quickAccess = quickAccess;
	}
		
	@JsonIgnore
	@AssertTrue(message = "Port nature must be web")
	public boolean isValidNature() {
	    return nature.equals("web");
	}
}