package fr.treeptik.cloudunit.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import fr.treeptik.cloudunit.config.events.ApplicationPendingEvent;
import fr.treeptik.cloudunit.config.events.ApplicationStartEvent;
import fr.treeptik.cloudunit.dto.DeploymentResource;
import fr.treeptik.cloudunit.exception.CheckException;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Deployment;
import fr.treeptik.cloudunit.model.DeploymentType;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.ApplicationService;
import fr.treeptik.cloudunit.service.DockerService;
import fr.treeptik.cloudunit.utils.AuthentificationUtils;

@Controller
@RequestMapping("/applications/{applicationId}/deployments")
public class DeploymentController {
    private final Logger logger = LoggerFactory.getLogger(DeploymentController.class);
    
    @Inject
    private ApplicationService applicationService;

    @Inject
    private AuthentificationUtils authentificationUtils;

    @Inject
    private DockerService dockerService;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;
    
    private DeploymentResource buildResource(Integer applicationId, Deployment deployment) {
        DeploymentResource resource = new DeploymentResource(deployment);
        
        try {
            resource.add(linkTo(methodOn(DeploymentController.class)
                        .getDeployment(applicationId, deployment.getContextPath()))
                    .withSelfRel());
            
            if (deployment.getType() == DeploymentType.WAR) {
                resource.add(new Link(deployment.getUri(), "open"));
            }
            
            resource.add(linkTo(methodOn(ApplicationController.class).detail(applicationId)).withRel("application"));
        } catch (ServiceException | CheckException e) {
            throw new RuntimeException(e);
        }
        
        return resource;
    }
    
    /**
     * Deploy a web application
     *
     * @return
     * @throws IOException
     * @throws ServiceException
     * @throws CheckException
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> deploy(@PathVariable Integer applicationId, @RequestParam("contextPath") String contextPath,
            @RequestPart("file") MultipartFile file)
            throws IOException, ServiceException, CheckException {
        User user = authentificationUtils.getAuthentificatedUser();
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        // We must be sure there is no running action before starting new one
        authentificationUtils.canStartNewAction(user, application, Locale.ENGLISH);

        Deployment deployment = applicationService.deploy(application, contextPath, file);

        String needRestart = dockerService.getEnv(application.getServer().getContainerID(),
                "CU_SERVER_RESTART_POST_DEPLOYMENT");
        if ("true".equalsIgnoreCase(needRestart)) {
            // set the application in pending mode
            applicationEventPublisher.publishEvent(new ApplicationPendingEvent(application));
            applicationService.stop(application);
            applicationService.start(application);
            // wait for modules and servers starting
            applicationEventPublisher.publishEvent(new ApplicationStartEvent(application));
        }

        logger.info("WAR deployed on {}", application.getName());
        
        DeploymentResource resource = buildResource(application.getId(), deployment);
        return ResponseEntity.created(URI.create(resource.getId().getHref())).body(resource);
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getDeployments(@PathVariable Integer applicationId) throws CheckException, ServiceException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Resources<DeploymentResource> resources = new Resources<>(
                application.getDeployments().stream()
                .map(d -> buildResource(applicationId, d))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(resources);
    }
    
    @RequestMapping(value = "/{contextPath}", method = RequestMethod.GET)
    public ResponseEntity<?> getDeployment(@PathVariable Integer applicationId, @PathVariable String contextPath)
            throws CheckException, ServiceException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Deployment> deployment = application.getDeployments().stream()
            .filter(d -> d.getContextPath().equals(contextPath))
            .findAny();
        
        if (!deployment.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        DeploymentResource resource = buildResource(application.getId(), deployment.get());
        
        return ResponseEntity.ok(resource);
    }
    
    // TODO undeploy
}
