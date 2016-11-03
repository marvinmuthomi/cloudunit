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
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import fr.treeptik.cloudunit.aspects.CloudUnitSecurable;
import fr.treeptik.cloudunit.config.events.ApplicationPendingEvent;
import fr.treeptik.cloudunit.config.events.ApplicationStartEvent;
import fr.treeptik.cloudunit.dto.ModulePortResource;
import fr.treeptik.cloudunit.dto.ModuleResource;
import fr.treeptik.cloudunit.exception.CheckException;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Module;
import fr.treeptik.cloudunit.model.Port;
import fr.treeptik.cloudunit.model.Status;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.ApplicationService;
import fr.treeptik.cloudunit.service.ModuleService;
import fr.treeptik.cloudunit.utils.AuthentificationUtils;

@Controller
@RequestMapping("/applications/{applicationId}/modules")
public class ModuleController {
    Locale locale = Locale.ENGLISH;

    private Logger logger = LoggerFactory.getLogger(ModuleController.class);

    @Inject
    private ModuleService moduleService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private AuthentificationUtils authentificationUtils;

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;
    
    private ModuleResource buildModuleResource(Application application, Module module) {
        ModuleResource resource = new ModuleResource(module);
        String moduleName = module.getImage().getName();
        Integer applicationId = application.getId();
        
        try {
            resource.add(linkTo(methodOn(ModuleController.class).getModule(applicationId, moduleName))
                    .withSelfRel());
            
            resource.add(linkTo(methodOn(ApplicationController.class).detail(applicationId))
                    .withRel("application"));
            
            resource.add(linkTo(methodOn(ModuleController.class).getPorts(applicationId, moduleName))
                    .withRel("ports"));
            
            resource.add(linkTo(methodOn(ContainerController.class).getContainer(applicationId, module.getName()))
                    .withRel("container"));
            
            resource.add(linkTo(methodOn(ModuleController.class).runScript(applicationId, moduleName, null))
                    .withRel("run-script"));
        } catch (CheckException | ServiceException e) {
            // ignore
        }
        
        return resource;
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getModules(@PathVariable Integer applicationId) throws CheckException, ServiceException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Resources<ModuleResource> resources = new Resources<>(
                application.getModules().stream()
                    .map(m -> buildModuleResource(application, m))
                    .collect(Collectors.toList()));
        
        resources.add(linkTo(methodOn(ModuleController.class).getModules(application.getId()))
                .withSelfRel());
        
        return ResponseEntity.ok(resources);
    }

    /**
     * Add a module to an existing application
     *
     * @param input
     * @return
     * @throws ServiceException
     * @throws CheckException
     */
    @CloudUnitSecurable
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> addModule(
            @PathVariable Integer applicationId,
            @Valid @RequestBody ModuleResource request)
            throws ServiceException, CheckException {
        String imageName = request.getName();

        User user = authentificationUtils.getAuthentificatedUser();
        Application application = applicationService.findById(applicationId);

        if (application == null) {
            return ResponseEntity.notFound().build();
        }

        applicationEventPublisher.publishEvent(new ApplicationPendingEvent(application));

        Module module;
        try {
            module = moduleService.create(imageName, application, user);
            logger.info("Module {} added to {}", imageName, application.getName());
        } finally {
            applicationEventPublisher.publishEvent(new ApplicationStartEvent(application));
        }
        
        ModuleResource resource = buildModuleResource(application, module);
        return ResponseEntity.created(URI.create(resource.getId().getHref())).body(resource);
    }
    
    @RequestMapping(value = "/{moduleName}", method = RequestMethod.GET)
    public ResponseEntity<?> getModule(@PathVariable Integer applicationId, @PathVariable String moduleName)
            throws CheckException, ServiceException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Module> module = application.getModules().stream()
            .filter(m -> m.getImage().getName().equals(moduleName))
            .findAny();
        
        if (!module.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ModuleResource resource = buildModuleResource(application, module.get());
        return ResponseEntity.ok(resource);
    }

    /**
     * Remove a module from an existing application
     */
    @CloudUnitSecurable
    @RequestMapping(value = "/{moduleName}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeModule(
            @PathVariable Integer applicationId,
            @PathVariable String moduleName) throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Module> module = application.getModules().stream()
            .filter(m -> m.getImage().getName().equals(moduleName))
            .findAny();
        
        if (!module.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        User user = authentificationUtils.getAuthentificatedUser();

        // We must be sure there is no running action before starting new one
        authentificationUtils.canStartNewAction(user, application, locale);

        Status previousApplicationStatus = application.getStatus();
        try {
            // Application occupée
            applicationService.setStatus(application, Status.PENDING);

            moduleService.remove(user, module.get(), true, previousApplicationStatus);
            logger.info("Module {} removed from {}", moduleName, application.getName());
        } catch (Exception e) {
            // Application en erreur
            logger.error(application.getName() + " // " + moduleName, e);
        } finally {
            applicationService.setStatus(application, previousApplicationStatus);
        }

        return ResponseEntity.noContent().build();
    }
    
    private ModulePortResource buildPortResource(Application application, Module module, Port port) {
        ModulePortResource resource = new ModulePortResource(port);
        
        try {
            resource.add(linkTo(methodOn(ModuleController.class).getPort(application.getId(), module.getImage().getName(), port.getContainerValue()))
                    .withSelfRel());
            
            resource.add(linkTo(methodOn(ModuleController.class).getModule(application.getId(), module.getImage().getName()))
                    .withRel("module"));
        } catch (CheckException | ServiceException e) {
            // ignore
        }
        
        return resource;
    }
    
    @CloudUnitSecurable
    @RequestMapping(value = "/{moduleName}/ports", method = RequestMethod.GET)
    public ResponseEntity<?> getPorts(
            @PathVariable Integer applicationId,
            @PathVariable String moduleName) throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Module> module = application.getModules().stream()
            .filter(m -> m.getImage().getName().equals(moduleName))
            .findAny();
        
        if (!module.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Resources<ModulePortResource> resources = new Resources<>(
                module.get().getPorts().stream()
                .map(p -> buildPortResource(application, module.get(), p))
                .collect(Collectors.toList()));
        
        return ResponseEntity.ok(resources);
    }
    
    @CloudUnitSecurable
    @RequestMapping(value = "/{moduleName}/ports/{portNumber}", method = RequestMethod.GET)
    public ResponseEntity<?> getPort(
            @PathVariable Integer applicationId,
            @PathVariable String moduleName,
            @PathVariable String portNumber) throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Module> module = application.getModules().stream()
            .filter(m -> m.getImage().getName().equals(moduleName))
            .findAny();
        
        if (!module.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Port> port = module.get().getPorts().stream()
                .filter(p -> p.getContainerValue().equals(portNumber))
                .findAny();
        
        if (!port.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ModulePortResource resource = buildPortResource(application, module.get(), port.get());
        return ResponseEntity.ok(resource);
    }

    /**
     * Remove a module from an existing application
     */
    @CloudUnitSecurable
    @RequestMapping(value = "/{moduleName}/ports/{portNumber}", method = RequestMethod.PUT)
    public ResponseEntity<?> publishPort(
            @PathVariable Integer applicationId,
            @PathVariable String moduleName,
            @PathVariable String portNumber,
            @Valid @RequestBody ModulePortResource request) throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Module> module = application.getModules().stream()
            .filter(m -> m.getImage().getName().equals(moduleName))
            .findAny();
        
        if (!module.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        boolean hasPort = module.get().getPorts().stream()
                .filter(p -> p.getContainerValue().equals(portNumber))
                .findAny()
                .isPresent();
        
        if (!hasPort) {
            return ResponseEntity.notFound().build();
        }

        User user = authentificationUtils.getAuthentificatedUser();

        // We must be sure there is no running action before starting new one
        authentificationUtils.canStartNewAction(user, application, locale);

        applicationEventPublisher.publishEvent(new ApplicationPendingEvent(application));
        Port port = moduleService.publishPort(module.get(), portNumber, request.getPublishPort(), user);
        applicationEventPublisher.publishEvent(new ApplicationStartEvent(application));
        
        ModulePortResource resource = buildPortResource(application, module.get(), port);
        return ResponseEntity.ok(resource);
    }
    
    @RequestMapping(value = "/{moduleName}/run-script", method = RequestMethod.POST,
            consumes = "multipart/form-data")
    public ResponseEntity<?> runScript(
            @PathVariable Integer applicationId,
            @PathVariable String moduleName,
            @RequestPart("file") MultipartFile file)
            throws ServiceException, CheckException {
        Application application = applicationService.findById(applicationId);
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Module> module = application.getModules().stream()
                .filter(m -> m.getImage().getName().equals(moduleName))
                .findAny();
        
        if (!module.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        String result = moduleService.runScript(module.get(), file);
        
        return ResponseEntity.ok(result);
    }

}
