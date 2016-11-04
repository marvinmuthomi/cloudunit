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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.treeptik.cloudunit.cli.CloudUnitCliException;
import fr.treeptik.cloudunit.cli.Messages;
import fr.treeptik.cloudunit.cli.exception.ManagerResponseException;
import fr.treeptik.cloudunit.cli.processor.InjectLogger;
import fr.treeptik.cloudunit.cli.rest.RestUtils;
import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.ModulePortResource;
import fr.treeptik.cloudunit.dto.ModuleResource;

@Component
public class ModuleUtils {
    private static final String MODULES_COUNT = Messages.getString("module.MODULES_COUNT");
    private static final String MODULE_REMOVED = Messages.getString("module.MODULE_REMOVED");
    private static final String MODULE_ADDED = Messages.getString("module.MODULE_ADDED");
    private static final String NO_SUCH_MODULE = Messages.getString("module.NO_SUCH_MODULE");
    private static final String NO_SUCH_PORT = Messages.getString("module.NO_SUCH_PORT");

    @Autowired
    private ApplicationUtils applicationUtils;

    @Autowired
    private AuthenticationUtils authenticationUtils;
    
    @Autowired
    private CheckUtils checkUtils;

    @InjectLogger
    private Logger log;

    @Autowired
    private RestUtils restUtils;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Resources<ModuleResource> getModules(ApplicationResource application) {
        String url = application.getLink("modules").getHref();
        
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            
            return objectMapper.readValue(response, new TypeReference<Resources<ModuleResource>>() {});
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get modules", e);
        }
    }
    
    public ModuleResource getModule(ApplicationResource application, String moduleName) {
        return getModules(application).getContent().stream()
                .filter(m -> m.getName().equals(moduleName))
                .findAny()
                .orElseThrow(() -> new CloudUnitCliException(MessageFormat.format(NO_SUCH_MODULE, application.getName(), moduleName)));
    }
    
    public ContainerResource getModuleContainer(ModuleResource module) {
        String url = module.getLink("container").getHref();
        
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            return objectMapper.readValue(response, ContainerResource.class);
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get module container", e);
        }
    }
    
    public Resources<ModulePortResource> getModulePorts(ModuleResource module) {
        String url = module.getLink("ports").getHref();
        
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            return objectMapper.readValue(response, new TypeReference<Resources<ModulePortResource>>() {});
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get module ports", e);
        }
    }
    
    public ModulePortResource getModulePort(ModuleResource module, String portNumber) {
        return getModulePorts(module).getContent().stream()
                .filter(p -> p.getNumber().equals(portNumber))
                .findAny()
                .orElseThrow(() -> new CloudUnitCliException(MessageFormat.format(NO_SUCH_PORT, portNumber)));
    }

    public String getListModules() {
        applicationUtils.checkConnectedAndApplicationSelected();
        
        Resources<ModuleResource> modules = getModules(applicationUtils.getCurrentApplication());
        MessageConverter.buildLightModuleMessage(modules.getContent());

        return MessageFormat.format(MODULES_COUNT, modules.getContent().size());
    }

    public String addModule(String imageName, File script) {
        applicationUtils.checkConnectedAndApplicationSelected();
        
        checkUtils.checkImageExists(imageName);
        
        ApplicationResource currentApplication = applicationUtils.getCurrentApplication();
        String url = currentApplication.getLink("modules").getHref();
        ModuleResource request = new ModuleResource();
        request.setName(imageName);
        
        try {
            String response = restUtils.sendPostCommand(url, authenticationUtils.getMap(), request);
            ModuleResource result = objectMapper.readValue(response, ModuleResource.class);
            
            return MessageFormat.format(MODULE_ADDED,
                    result.getName(),
                    currentApplication.getName());

        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't add module", e);
        }        
    }

    public String removeModule(String moduleName) {
        applicationUtils.checkConnectedAndApplicationSelected();

        ModuleResource module = getModule(applicationUtils.getCurrentApplication(), moduleName);
        
        String url = module.getId().getHref();
        try {
            restUtils.sendDeleteCommand(url, authenticationUtils.getMap());
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't remove module", e);
        }

        return MessageFormat.format(MODULE_REMOVED,
                moduleName,
                applicationUtils.getCurrentApplication().getName());
    }

    public String managePort(String moduleName, String portNumber, boolean open) {
        applicationUtils.checkConnectedAndApplicationSelected();
        
        ModuleResource module = getModule(applicationUtils.getCurrentApplication(), moduleName);
        ModulePortResource port = getModulePort(module, portNumber);
        
        port.setPublishPort(open);
        
        String url = port.getId().getHref();
        try {
            restUtils.sendPutCommand(url, authenticationUtils.getMap(), port);
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't change port", e);
        }

        return "OK";
    }

    public String runScript(String moduleName, File file) {
        applicationUtils.checkConnectedAndApplicationSelected();
        
        ModuleResource module = getModule(applicationUtils.getCurrentApplication(), moduleName);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("file", new FileSystemResource(file));
        parameters.putAll(authenticationUtils.getMap());
        
        String url = module.getLink("run-script").getHref();
        
        log.info("Running script...");
        
        restUtils.sendPostForUpload(url, parameters);
        
        return "Done";
    }

}
