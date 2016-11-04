/*
 * LICENCE : CloudUnit is available under the GNU Affero General Public License : https://gnu.org/licenses/agpl.html
 *     but CloudUnit is licensed too under a standard commercial license.
 *     Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 *     If you are not sure whether the GPL is right for you,
 *     you can always test our software under the GPL and inspect the source code before you contact us
 *     about purchasing a commercial license.
 *
 *     LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 *     or promote products derived from this project without prior written permission from Treeptik.
 *     Products or services derived from this software may not be called "CloudUnit"
 *     nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 *     For any questions, contact us : contact@treeptik.fr
 */

package fr.treeptik.cloudunit.cli.utils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.treeptik.cloudunit.cli.CloudUnitCliException;
import fr.treeptik.cloudunit.cli.exception.ManagerResponseException;
import fr.treeptik.cloudunit.cli.processor.InjectLogger;
import fr.treeptik.cloudunit.cli.rest.RestUtils;
import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.PortResource;
import fr.treeptik.cloudunit.dto.ServerResource;

@Component
public class ServerUtils {
	private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectLogger
	private Logger log;

	@Autowired
	private AuthenticationUtils authenticationUtils;

	@Autowired
	private ApplicationUtils applicationUtils;

	@Autowired
	private RestUtils restUtils;

	private List<String> availableJavaVersion = Arrays.asList(new String[] { "jdk1.7.0_55", "jdk1.8.0_25" });

	private List<Long> availableMemoryValues = Arrays.asList(512L, 1024L, 2048L, 3072L);

	/**
	 * @param memory
	 * @return
	 */
	public String changeMemory(long memory) {
	    applicationUtils.checkApplicationSelected();

		if (!availableMemoryValues.contains(memory)) {
			throw new CloudUnitCliException("The memory value you have put is not authorized (512, 1024, 2048, 3072)");
		}
		
		String url = applicationUtils.getCurrentApplication().getLink("server").getHref();
		
		ServerResource request = new ServerResource();
		request.setJvmMemory(memory);
		try {
			restUtils.sendPatchCommand(url, authenticationUtils.getMap(), request);
		} catch (ManagerResponseException e) {
		    throw new CloudUnitCliException("Couldn't change memory", e);
		}

		return "OK";
	}

	/**
	 * Add an option for JVM
	 *
	 * @param opts
	 * @return
	 */
	public String addOpts(String opts) {
        applicationUtils.checkApplicationSelected();

        ServerResource server = applicationUtils.getServer(applicationUtils.getCurrentApplication());
        
        String url = server.getId().getHref();
        
        ServerResource request = new ServerResource();
        request.setJvmOptions(String.join(" ", server.getJvmOptions(), opts));
        
		try {
			restUtils.sendPatchCommand(url, authenticationUtils.getMap(), request);
		} catch (ManagerResponseException e) {
			throw new CloudUnitCliException("Couldn't add JVM option", e);
		}

		return "OK";
	}

	/**
	 * Change JVM Release
	 *
	 * @param applicationName
	 * @param jvmRelease
	 * @return
	 */
	public String changeJavaVersion(String applicationName, String jvmRelease) {
	    ApplicationResource application = applicationUtils.getSpecificOrCurrentApplication(applicationName);
	    
		if (!availableJavaVersion.contains(jvmRelease)) {
			throw new CloudUnitCliException("The specified java version is not available");
		}

		String url = application.getLink("server").getHref();
		
		ServerResource request = new ServerResource();
		request.setJvmRelease(jvmRelease);
		try {
			restUtils.sendPatchCommand(url, authenticationUtils.getMap(), request);
		} catch (ManagerResponseException e) {
		    throw new CloudUnitCliException("Couldn't change Java version", e);
		}

		return "OK";
	}

	public String openPort(String applicationName, int portToOpen, String portNature) {
	    ApplicationResource application = applicationUtils.getSpecificOrCurrentApplication(applicationName);

	    String url = application.getLink("ports").getHref();
	    
	    PortResource request = new PortResource();
	    request.setNumber(portToOpen);
	    request.setNature(portNature);

		try {
			String response = restUtils.sendPostCommand(url, authenticationUtils.getMap(), request);
			PortResource result = objectMapper.readValue(response, PortResource.class);
			
			return MessageFormat.format("Port open and available at {0}", result.getLink("open").getHref());
		} catch (ManagerResponseException | IOException e) {
		    throw new CloudUnitCliException("Couldn't open port");
		}
	}
	
	public Resources<PortResource> getPorts(ApplicationResource application) {
	    String url = application.getLink("ports").getHref();
	    
	    try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            return objectMapper.readValue(response, new TypeReference<Resources<PortResource>>() {});
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get ports", e);
        }
	}

	public PortResource getPort(ApplicationResource application, int portNumber) {
	    return getPorts(application).getContent().stream()
	            .filter(p -> p.getNumber().equals(portNumber))
	            .findAny()
	            .orElseThrow(() -> new CloudUnitCliException(MessageFormat.format("No such port \"{0}\"", portNumber)));
	}
	
	/**
	 * @param applicationName
	 * @param portToOpen
	 * @return
	 */
	public String removePort(String applicationName, int portNumber) {
        ApplicationResource application = applicationUtils.getSpecificOrCurrentApplication(applicationName);
        
        PortResource port = getPort(application, portNumber);

        String url = port.getId().getHref();
		try {
			restUtils.sendDeleteCommand(url, authenticationUtils.getMap());
		} catch (ManagerResponseException e) {
		    throw new CloudUnitCliException("Couldn't remove port", e);
		}

		return "OK";
	}

	public String mountVolume(String name, String path, Boolean mode, String containerName, String applicationName) {
	    ApplicationResource application = applicationUtils.getSpecificOrCurrentApplication(applicationName);
	    
		if (containerName == null)
		{
			containerName = applicationUtils.getServerContainer(applicationUtils.getServer(application)).getName();
		}

		try {
			Map<String, String> parameters = new HashMap<>();
			parameters.put("containerName", containerName);
			parameters.put("path", path);

			if(mode.equals(true)) parameters.put("mode", "ro");
			else parameters.put("mode", "rw");

			parameters.put("volumeName", name);
			parameters.put("applicationName", application.getName());
			restUtils.sendPutCommand(authenticationUtils.finalHost + "/server/volume", authenticationUtils.getMap(), parameters);
		} catch (ManagerResponseException e) {
		    throw new CloudUnitCliException("Couldn't mount volume", e);
		}

		return "This volume has successful been mounted";
	}

	public String unmountVolume(String name, String containerName) {
		try {
			restUtils.sendDeleteCommand(authenticationUtils.finalHost + "/server/volume/" + name + "/container/" +
					containerName, authenticationUtils.getMap());
		} catch (ManagerResponseException e) {
		    throw new CloudUnitCliException("Couldn't unmount volume", e);
		}

		return "This volume has successful been unmounted";
	}

}
