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

package fr.treeptik.cloudunit.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.treeptik.cloudunit.aspects.CloudUnitSecurable;
import fr.treeptik.cloudunit.config.events.ApplicationFailEvent;
import fr.treeptik.cloudunit.config.events.ApplicationPendingEvent;
import fr.treeptik.cloudunit.config.events.ApplicationStartEvent;
import fr.treeptik.cloudunit.config.events.ApplicationStopEvent;
import fr.treeptik.cloudunit.dto.AliasResource;
import fr.treeptik.cloudunit.dto.ApplicationCreationRequest;
import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.ContainerUnit;
import fr.treeptik.cloudunit.dto.EnvUnit;
import fr.treeptik.cloudunit.dto.HttpOk;
import fr.treeptik.cloudunit.dto.JsonInput;
import fr.treeptik.cloudunit.dto.JsonResponse;
import fr.treeptik.cloudunit.dto.PortResource;
import fr.treeptik.cloudunit.enums.RemoteExecAction;
import fr.treeptik.cloudunit.exception.CheckException;
import fr.treeptik.cloudunit.exception.FatalDockerJSONException;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.factory.EnvUnitFactory;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.PortToOpen;
import fr.treeptik.cloudunit.model.Status;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.ApplicationService;
import fr.treeptik.cloudunit.service.DockerService;
import fr.treeptik.cloudunit.utils.AuthentificationUtils;
import fr.treeptik.cloudunit.utils.CheckUtils;

/**
 * Controller about Application lifecycle Application is the main concept for
 * CloudUnit : it composed by Server, Module and Metadata
 */
@Controller
@RequestMapping("/applications")
public class ApplicationController {
	private final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

	@Inject
	private ApplicationService applicationService;

	@Inject
	private AuthentificationUtils authentificationUtils;

	@Inject
	private DockerService dockerService;

	@Inject
	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * CREATE AN APPLICATION
	 *
	 * @param input
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 * @throws InterruptedException
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<?> createApplication(@Valid @RequestBody ApplicationCreationRequest request)
			throws ServiceException, CheckException, InterruptedException {
		// We must be sure there is no running action before starting new one
		User user = authentificationUtils.getAuthentificatedUser();
		authentificationUtils.canStartNewAction(user, null, Locale.ENGLISH);

		Application application = applicationService.create(request.getName(), request.getDisplayName(),
		        user.getLogin(), request.getServerType(), null, null);

		ApplicationResource resource = buildResource(application);
		return ResponseEntity.created(URI.create(resource.getId().getHref())).body(resource);
	}

	/**
	 * START AN APPLICATION
	 *
	 * @param input
	 *            {applicatioName:myApp-johndoe-admin}
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 * @throws InterruptedException
	 */
	@CloudUnitSecurable
	@RequestMapping(value = "/{applicationId}/restart", method = RequestMethod.POST)
	public ResponseEntity<?> restartApplication(@PathVariable Integer applicationId)
			throws ServiceException, CheckException {

		User user = authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findById(applicationId);
		
		if (application == null || application.getStatus().equals(Status.PENDING)) {
		    return ResponseEntity.notFound().build();
		}

		// We must be sure there is no running action before starting new one
		authentificationUtils.canStartNewAction(user, application, Locale.ENGLISH);

		if (application.getStatus().equals(Status.START)) {
			applicationService.stop(application);
			applicationService.start(application);
		} else if (application.getStatus().equals(Status.STOP)) {
			applicationService.start(application);
		}

		return ResponseEntity.noContent().build();
	}

	/**
	 * START AN APPLICATION
	 *
	 * @param input
	 *            {applicatioName:myApp-johndoe-admin}
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 * @throws InterruptedException
	 */
	@CloudUnitSecurable
	@RequestMapping(value = "/{applicationId}/start", method = RequestMethod.POST)
	public ResponseEntity<?> startApplication(@PathVariable Integer applicationId)
			throws ServiceException, CheckException {
		Application application = applicationService.findById(applicationId);

		if (application == null || application.getStatus().equals(Status.START)) {
			return ResponseEntity.notFound().build();
		}

		User user = authentificationUtils.getAuthentificatedUser();
		// We must be sure there is no running action before starting new one
		authentificationUtils.canStartNewAction(user, application, Locale.ENGLISH);

		// set the application in pending mode
		applicationEventPublisher.publishEvent(new ApplicationPendingEvent(application));

		applicationService.start(application);

		// wait for modules and servers starting
		applicationEventPublisher.publishEvent(new ApplicationStartEvent(application));

		return ResponseEntity.noContent().build();
	}

	/**
	 * STOP a running application
	 *
	 * @param input
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@RequestMapping(value = "/{applicationId}/stop", method = RequestMethod.POST)
	public ResponseEntity<?> stopApplication(@PathVariable Integer applicationId)
	        throws ServiceException, CheckException {
		User user = authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findById(applicationId);
		
		if (application == null || application.getStatus() != Status.START) {
		    return ResponseEntity.notFound().build();
		}

		// We must be sure there is no running action before starting new one
		authentificationUtils.canStartNewAction(user, application, Locale.ENGLISH);

		// set the application in pending mode
		applicationEventPublisher.publishEvent(new ApplicationPendingEvent(application));

		// stop the application
		applicationService.stop(application);

		applicationEventPublisher.publishEvent(new ApplicationStopEvent(application));
        
        return ResponseEntity.noContent().build();
	}

	/**
	 * DELETE AN APPLICATION
	 *
	 * @param jsonInput
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@RequestMapping(value = "/{applicationId}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteApplication(@PathVariable Integer applicationId)
	        throws ServiceException, CheckException {
	    User user = this.authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findById(applicationId);

		// We must be sure there is no running action before starting new one
		authentificationUtils.canStartDeleteApplicationAction(user, application, Locale.ENGLISH);

		try {
			// Application busy
			// set the application in pending mode
			applicationEventPublisher.publishEvent(new ApplicationPendingEvent(application));

			logger.info("Removing application: {}", application.getName());
			applicationService.remove(application, user);

		} catch (ServiceException e) {
			// set the application in pending mode
			applicationEventPublisher.publishEvent(new ApplicationFailEvent(application));
		}

		logger.info("Application {} is deleted.", application.getName());

		return ResponseEntity.noContent().build();
	}
	
	private ApplicationResource buildResource(Application application) {
	    ApplicationResource resource = new ApplicationResource(application);
	    
	    try {
    	    resource.add(linkTo(methodOn(ApplicationController.class).detail(application.getId()))
    	            .withSelfRel());
    	    
    	    resource.add(linkTo(methodOn(ServerController.class).getServer(application.getId()))
    	            .withRel("server"));
    	    
    	    resource.add(linkTo(methodOn(DeploymentController.class).getDeployments(application.getId()))
    	            .withRel("deployments"));
    	    
    	    resource.add(linkTo(methodOn(ApplicationController.class).aliases(application.getId()))
    	            .withRel("aliases"));
    	    
    	    if (EnumSet.of(Status.START, Status.STOP).contains(application.getStatus())) {
                resource.add(linkTo(methodOn(ApplicationController.class).restartApplication(application.getId()))
                        .withRel("restart"));
                
    	        if (application.getStatus() == Status.START) {
    	            resource.add(linkTo(methodOn(ApplicationController.class).stopApplication(application.getId()))
    	                    .withRel("stop"));
    	        } else {
    	            resource.add(linkTo(methodOn(ApplicationController.class).startApplication(application.getId()))
    	                    .withRel("start"));
    	        }
    	    }
	    } catch (ServiceException | CheckException e) {
	        // TODO deal with exception architecture
	        throw new RuntimeException(e);
	    }
	    
	    return resource;
	}

	/**
	 * Return detailed information about application
	 *
	 * @return
	 * @throws ServiceException
	 */
	@CloudUnitSecurable
	@RequestMapping(value = "/{applicationId}", method = RequestMethod.GET)
	public ResponseEntity<?> detail(@PathVariable Integer applicationId) throws ServiceException, CheckException {
		Application application = applicationService.findById(applicationId);
		
		if (application == null) {
		    return ResponseEntity.notFound().build();
		}
		
		ApplicationResource resource = buildResource(application);
		return ResponseEntity.ok(resource);
	}

	/**
	 * Return the list of applications for an User
	 *
	 * @return
	 * @throws ServiceException
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<?> findAllByUser() throws ServiceException {
		User user = this.authentificationUtils.getAuthentificatedUser();
		List<Application> applications = applicationService.findAllByUser(user);

		logger.debug("Number of applications {}", applications.size());
		
		Resources<ApplicationResource> resources = new Resources<>(
		        applications.stream()
		        .map(this::buildResource)
		        .collect(Collectors.toList()));
		
		resources.add(linkTo(methodOn(ApplicationController.class).findAllByUser()).withSelfRel());
		
		return ResponseEntity.ok(resources);
	}

	/**
	 * Return the list of containers for an application (module, server or
	 * tools)
	 *
	 * @param applicationName
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@ResponseBody
	@RequestMapping(value = "/{applicationName}/containers", method = RequestMethod.GET)
	public List<ContainerUnit> listContainer(@PathVariable String applicationName)
			throws ServiceException, CheckException {
		logger.debug("applicationName:" + applicationName);
		return applicationService.listContainers(applicationName);
	}
	
	private AliasResource buildAliasResource(Application application, String alias) {
	    AliasResource resource = new AliasResource(alias);
	    
	    try {
            resource.add(linkTo(methodOn(ApplicationController.class).alias(application.getId(), alias))
                    .withSelfRel());
        } catch (CheckException | ServiceException e) {
            // ignore
        }
	    
	    return resource;
	}

	/**
	 * Return the list of aliases for an application
	 *
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@ResponseBody
	@RequestMapping(value = "/{applicationId}/aliases", method = RequestMethod.GET)
	public ResponseEntity<?> aliases(@PathVariable Integer applicationId) throws ServiceException, CheckException {
		Application application = applicationService.findById(applicationId);
		
		if (application == null) {
		    ResponseEntity.notFound().build();
		}
		
		logger.debug("application name = {}", application.getName());
		
		List<String> aliases = applicationService.getListAliases(application);
		
		if (aliases == null) {
		    ResponseEntity.notFound().build();
		}
		
		Resources<AliasResource> resources = new Resources<>(
		        aliases.stream()
		        .map(a -> buildAliasResource(application, a))
		        .collect(Collectors.toList()));
		
		resources.add(linkTo(methodOn(ApplicationController.class).aliases(applicationId)).withSelfRel());
		
		return ResponseEntity.ok(resources);
	}

	/**
	 * Add an alias for an application
	 *
	 * @param input
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@ResponseBody
	@RequestMapping(value = "/{applicationId}/aliases", method = RequestMethod.POST)
	public ResponseEntity<?> addAlias(@PathVariable Integer applicationId, @Valid @RequestBody AliasResource request)
	        throws ServiceException, CheckException {
		User user = this.authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findById(applicationId);

		// We must be sure there is no running action before starting new one
		authentificationUtils.canStartNewAction(user, application, Locale.ENGLISH);

		String alias = applicationService.addNewAlias(application, request.getName());
		
		AliasResource resource = buildAliasResource(application, alias);
		
		return ResponseEntity.created(URI.create(resource.getId().getHref())).body(resource);
	}
	
    @RequestMapping(value = "/{applicationId}/aliases/{aliasName}", method = RequestMethod.GET)
    public ResponseEntity<?> alias(@PathVariable Integer applicationId, @PathVariable String aliasName)
            throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            ResponseEntity.notFound().build();
        }
        
        logger.debug("application name = {}", application.getName());
        
        List<String> aliases = applicationService.getListAliases(application);
        
        if (aliases == null) {
            ResponseEntity.notFound().build();
        }
        
        Optional<String> alias = aliases.stream()
                .filter(a -> a.equals(aliasName))
                .findAny();
        
        if (!alias.isPresent()) {
            ResponseEntity.notFound().build();
        }
        
        AliasResource resource = buildAliasResource(application, alias.get());
        return ResponseEntity.ok(resource);
    }	

	/**
	 * Delete an alias for an application
	 *
	 * @param jsonInput
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@ResponseBody
	@RequestMapping(value = "/{applicationId}/aliases/{aliasName}", method = RequestMethod.DELETE)
	public ResponseEntity<?> removeAlias(@PathVariable Integer applicationId, @PathVariable String aliasName)
	        throws ServiceException, CheckException {
		logger.debug("application id: {}; alias name: {}", applicationId, aliasName);

		User user = this.authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findById(applicationId);
		
        if (application == null) {
            ResponseEntity.notFound().build();
        }
        
        List<String> aliases = applicationService.getListAliases(application);
        
        if (aliases == null) {
            ResponseEntity.notFound().build();
        }
        
        Optional<String> alias = aliases.stream()
                .filter(a -> a.equals(aliasName))
                .findAny();
        
        if (!alias.isPresent()) {
            ResponseEntity.notFound().build();
        }

		// We must be sure there is no running action before starting new one
		authentificationUtils.canStartNewAction(user, null, Locale.ENGLISH);

		applicationService.removeAlias(application, aliasName);

		return ResponseEntity.noContent().build();
	}

	/**
	 * Add a port for an application
	 *
	 * @param input
	 * 
	 * @return
	 * 
	 * @throws ServiceException
	 * 
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@ResponseBody
	@RequestMapping(value = "/ports", method = RequestMethod.POST)
	public ResponseEntity<PortResource> addPort(@RequestBody JsonInput input) throws ServiceException, CheckException {

		if (logger.isDebugEnabled()) {
			logger.debug(input.toString());
		}

		String applicationName = input.getApplicationName();
		String nature = input.getPortNature();
		Boolean isQuickAccess = input.getPortQuickAccess();
		
		User user = this.authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findByNameAndUser(user, applicationName);

		CheckUtils.validateOpenPort(input.getPortToOpen(), application);
		CheckUtils.isPortFree(input.getPortToOpen(), application);
		CheckUtils.validateNatureForOpenPortFeature(input.getPortNature(), application);

		Integer port = Integer.parseInt(input.getPortToOpen());
		PortToOpen portToOpen= applicationService.addPort(application, nature, port, isQuickAccess);
		PortResource portResource = new PortResource(portToOpen);
		return ResponseEntity.status(HttpStatus.OK).body(portResource);
	}

	/**
	 * Delete a port for an application
	 *
	 * @param input
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@ResponseBody
	@RequestMapping(value = "/{applicationName}/ports/{portToOpen}", method = RequestMethod.DELETE)
	public JsonResponse removePort(JsonInput input) throws ServiceException, CheckException {

		String applicationName = input.getApplicationName();

		if (logger.isDebugEnabled()) {
			logger.debug("application.name=" + applicationName);
			logger.debug("application.port=" + input.getPortToOpen());
		}

		User user = this.authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findByNameAndUser(user, applicationName);

		CheckUtils.validateOpenPort(input.getPortToOpen(), application);
		Integer port = Integer.parseInt(input.getPortToOpen());
		applicationService.removePort(application, port);

		return new HttpOk();
	}

	/**
	 * Display env variables for a container
	 *
	 * @param applicationName
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@ResponseBody
	@RequestMapping(value = "/{applicationName}/container/{containerName}/env", method = RequestMethod.GET)
	public List<EnvUnit> displayEnv(@PathVariable String applicationName, @PathVariable String containerName)
			throws ServiceException, CheckException {
		List<EnvUnit> envUnits = null;
		try {
			User user = this.authentificationUtils.getAuthentificatedUser();
			String content = dockerService.execCommand(containerName,
					RemoteExecAction.GATHER_CU_ENV.getCommand() + " " + user.getLogin());
			logger.debug(content);
			envUnits = EnvUnitFactory.fromOutput(content);
		} catch (FatalDockerJSONException e) {
			throw new ServiceException(applicationName + ", " + containerName, e);
		}
		return envUnits;
	}
}