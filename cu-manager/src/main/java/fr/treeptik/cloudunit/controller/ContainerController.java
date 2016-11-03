package fr.treeptik.cloudunit.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.treeptik.cloudunit.aspects.CloudUnitSecurable;
import fr.treeptik.cloudunit.config.events.ApplicationStartEvent;
import fr.treeptik.cloudunit.config.events.ServerStartEvent;
import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.EnvironmentVariableResource;
import fr.treeptik.cloudunit.enums.RemoteExecAction;
import fr.treeptik.cloudunit.exception.CheckException;
import fr.treeptik.cloudunit.exception.FatalDockerJSONException;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Container;
import fr.treeptik.cloudunit.model.EnvironmentVariable;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.ApplicationService;
import fr.treeptik.cloudunit.service.DockerService;
import fr.treeptik.cloudunit.service.EnvironmentService;
import fr.treeptik.cloudunit.utils.AuthentificationUtils;

@Controller
@RequestMapping("/applications/{applicationId}/containers")
public class ContainerController {
    @Inject
    private ApplicationService applicationService;
    
    @Inject
    private EnvironmentService environmentService;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private AuthentificationUtils authentificationUtils;

    @Inject
    private DockerService dockerService;
    
    private ContainerResource buildContainerResource(Application application, Container container) {
        ContainerResource resource = new ContainerResource(container);
        
        try {
            Integer applicationId = application.getId();
            String containerName = container.getName();
            resource.add(linkTo(methodOn(ContainerController.class).getContainer(applicationId, containerName))
                    .withSelfRel());
            
            resource.add(linkTo(methodOn(ContainerController.class).displayEnv(applicationId, containerName))
                    .withRel("env"));
            
            resource.add(linkTo(methodOn(ContainerController.class).loadAllEnvironmentVariables(applicationId, containerName))
                    .withRel("env-vars"));
            
            resource.add(linkTo(methodOn(ApplicationController.class).detail(applicationId))
                    .withRel("application"));
        } catch (CheckException | ServiceException e) {
            // ignore
        }
        
        return resource;
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getContainers(@PathVariable Integer applicationId)
            throws CheckException, ServiceException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Resources<ContainerResource> resources = new Resources<>(
                application.getContainers().stream()
                .map(c -> buildContainerResource(application, c))
                .collect(Collectors.toList()));
        
        resources.add(linkTo(methodOn(ContainerController.class).getContainers(application.getId()))
                .withSelfRel());
        
        return ResponseEntity.ok(resources);
    }
    
    @RequestMapping(value = "/{containerName}", method = RequestMethod.GET)
    public ResponseEntity<?> getContainer(@PathVariable Integer applicationId, @PathVariable String containerName)
            throws CheckException, ServiceException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<Container> container = application.getContainers().stream()
            .filter(c -> c.getName().equals(containerName))
            .findAny();
        
        if (!container.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ContainerResource resource = buildContainerResource(application, container.get());
        return ResponseEntity.ok(resource);
    }
    
    @CloudUnitSecurable
    @RequestMapping(value = "/{containerName}/env", method = RequestMethod.GET)
    public ResponseEntity<?> displayEnv(
            @PathVariable Integer applicationId,
            @PathVariable String containerName)
            throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }

        User user = authentificationUtils.getAuthentificatedUser();
        try {
            String content = dockerService.execCommand(containerName,
                    RemoteExecAction.GATHER_CU_ENV.getCommand() + " " + user.getLogin());
            
            Resources<EnvironmentVariableResource> resources = new Resources<>(
                    EnvironmentVariableResource.fromDefinitions(content));
            
            resources.add(linkTo(methodOn(ContainerController.class).displayEnv(application.getId(), containerName))
                    .withSelfRel());
            
            return ResponseEntity.ok(resources);
        } catch (FatalDockerJSONException e) {
            throw new ServiceException(applicationId + ", " + containerName, e);
        }
    }
    
    public EnvironmentVariableResource buildVariableResource(Application application, String containerName,
            EnvironmentVariable variable) {
        EnvironmentVariableResource resource = new EnvironmentVariableResource(variable);
        
        Integer applicationId = application.getId();
        
        try {
            resource.add(linkTo(methodOn(ContainerController.class).loadEnvironmentVariable(applicationId, containerName, variable.getId()))
                    .withSelfRel());
        } catch (CheckException | ServiceException e) {
            // ignore
        }
        
        return resource;
    }
    
    @CloudUnitSecurable
    @RequestMapping(value = "/{containerName}/env-vars", method = RequestMethod.POST)
    public ResponseEntity<?> addEnvironmentVariable(
            @PathVariable Integer applicationId,
            @PathVariable String containerName,
            @Valid @RequestBody EnvironmentVariableResource request)
            throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        EnvironmentVariable variable = environmentService.save(application, containerName,
                request.getName(), request.getValue());
        applicationEventPublisher.publishEvent(new ServerStartEvent(application.getServer()));
        applicationEventPublisher.publishEvent(new ApplicationStartEvent(application));
        
        EnvironmentVariableResource resource = buildVariableResource(application, containerName, variable);
        return ResponseEntity.created(URI.create(resource.getId().getHref())).body(resource);
    }

    @CloudUnitSecurable
    @RequestMapping(value = "/{containerName}/env-vars", method = RequestMethod.GET)
    public ResponseEntity<?> loadAllEnvironmentVariables(
            @PathVariable Integer applicationId,
            @PathVariable String containerName)
            throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Resources<EnvironmentVariableResource> resources = new Resources<>(
                environmentService.loadEnvironnmentsByContainer(containerName).stream()
                .map(v -> buildVariableResource(application, containerName, v))
                .collect(Collectors.toList()));
        
        resources.add(linkTo(methodOn(ContainerController.class).loadAllEnvironmentVariables(application.getId(), containerName))
                .withSelfRel());
        return ResponseEntity.ok(resources);
    }

    @CloudUnitSecurable
    @RequestMapping(value = "/{containerName}/env-vars/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> loadEnvironmentVariable(
            @PathVariable Integer applicationId,
            @PathVariable String containerName,
            @PathVariable int id) throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        EnvironmentVariable variable = environmentService.loadEnvironnment(id);
        
        EnvironmentVariableResource resource = buildVariableResource(application, containerName, variable);
        return ResponseEntity.ok(resource);
    }

    @CloudUnitSecurable
    @RequestMapping(value = "/{containerName}/env-vars/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateEnvironmentVariable(
            @PathVariable Integer applicationId,
            @PathVariable String containerName,
            @PathVariable int id,
            @Valid @RequestBody EnvironmentVariableResource request) throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        EnvironmentVariable variable = environmentService.update(application, containerName, id, request.getValue());
        EnvironmentVariableResource resource = buildVariableResource(application, containerName, variable);
        return ResponseEntity.ok(resource);
    }

    @CloudUnitSecurable
    @RequestMapping(value = "/{containerName}/env-vars/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteEnvironmentVariable(
            @PathVariable Integer applicationId,
            @PathVariable String containerName,
            @PathVariable int id) throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        environmentService.delete(application, containerName, id);
        return ResponseEntity.noContent().build();
    }
}
