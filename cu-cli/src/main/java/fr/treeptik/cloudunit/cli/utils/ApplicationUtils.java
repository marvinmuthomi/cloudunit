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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.treeptik.cloudunit.cli.CloudUnitCliException;
import fr.treeptik.cloudunit.cli.Guard;
import fr.treeptik.cloudunit.cli.Messages;
import fr.treeptik.cloudunit.cli.commands.ShellStatusCommand;
import fr.treeptik.cloudunit.cli.exception.ManagerResponseException;
import fr.treeptik.cloudunit.cli.model.ModuleAndContainer;
import fr.treeptik.cloudunit.cli.model.ServerAndContainer;
import fr.treeptik.cloudunit.cli.processor.InjectLogger;
import fr.treeptik.cloudunit.cli.rest.JsonConverter;
import fr.treeptik.cloudunit.cli.rest.RestUtils;
import fr.treeptik.cloudunit.dto.AliasResource;
import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.Command;
import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.DeploymentResource;
import fr.treeptik.cloudunit.dto.EnvironmentVariableResource;
import fr.treeptik.cloudunit.dto.ModuleResource;
import fr.treeptik.cloudunit.dto.ServerResource;

@Component
public class ApplicationUtils {
    private static final String ENV_VAR_ADDED = "Environment variable \"{0}\" has been added to application \"{1}\"";
    private static final String ENV_VAR_REMOVED = "Environment variable \"{0}\" has been removed from application \"{1}\"";
    private static final String NO_SUCH_ENV_VAR = "No such environment variable \"{0}\"";
    private static final String APPLICATION_CREATED = "Application \"{0}\" has been created";
    private static final String APPLICATION_REMOVED = "Application \"{0}\" has been removed";
    private static final String APPLICATION_STARTED = "Application \"{0}\" has been started";
    private static final String APPLICATION_STOPPED = "Application \"{0}\" has been stopped";
    private static final String APPLICATION_COUNT = Messages.getString("application.APPLICATION_COUNT");
    private static final String NO_APPLICATION = Messages.getString("application.NO_APPLICATION");
    private static final String NO_SUCH_APPLICATION = Messages.getString("application.NO_SUCH_APPLICATION");
    private static final String ALIAS_COUNT = Messages.getString("application.ALIAS_COUNT");
    private static final String ALIAS_ADDED = Messages.getString("application.ALIAS_ADDED");
    private static final String ALIAS_REMOVED = Messages.getString("application.ALIAS_REMOVED");
    private static final String NO_SUCH_ALIAS = Messages.getString("application.NO_SUCH_ALIAS");
    private static final String ENV_VARS_COUNT = Messages.getString("application.ENV_VARS_COUNT");
    private static final String NO_SUCH_CONTAINER = Messages.getString("application.NO_SUCH_CONTAINER");
    
    public static final String APPLICATIONS_URL_FORMAT = "%s/applications";

    @InjectLogger
    private Logger log;

    @Autowired
    private UrlLoader urlLoader;

    @Autowired
    private AuthenticationUtils authenticationUtils;

    @Autowired
    private ShellStatusCommand statusCommand;

    @Autowired
    private RestUtils restUtils;

    @Autowired
    private CheckUtils checkUtils;

    @Autowired
    private FileUtils fileUtils;
    
    @Autowired
    private ModuleUtils moduleUtils;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApplicationResource currentApplication;

    public ApplicationResource getCurrentApplication() {
        return currentApplication;
    }

    public void setCurrentApplication(ApplicationResource application) {
        this.currentApplication = application;
    }

    public boolean isApplicationSelected() {
        return currentApplication != null;
    }
    
    public void checkApplicationSelected() {
        Guard.guardTrue(isApplicationSelected(), NO_APPLICATION);
        currentApplication = refreshApplication(currentApplication);
    }
    
    public void checkConnectedAndApplicationSelected() {
        authenticationUtils.checkConnected();
        checkApplicationSelected();
        fileUtils.checkNotInFileExplorer();
    }

    public boolean applicationExists(String applicationName) {
        try {
            return listAllApps().getContent().stream()
                    .map(app -> app.getName())
                    .filter(name -> name.equals(applicationName))
                    .findAny()
                    .isPresent();
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't check for application", e);
        }
    }
    
    public void checkApplicationExists(String applicationName) {
        Guard.guardTrue(applicationExists(applicationName), NO_SUCH_APPLICATION, applicationName);
    }

    public String getInformations() {
        checkConnectedAndApplicationSelected();
        
        ServerResource server = getServer(currentApplication);
        ContainerResource container = getServerContainer(server);
        
        ServerAndContainer serverAndContainer = new ServerAndContainer(server, container);
        
        Collection<ModuleResource> modules = moduleUtils.getModules(currentApplication).getContent();
        
        Collection<ModuleAndContainer> modulesAndContainers = modules.stream()
                .map(m -> new ModuleAndContainer(
                        m,
                        moduleUtils.getModuleContainer(m),
                        moduleUtils.getModulePorts(m).getContent()))
                .collect(Collectors.toList());

        MessageConverter.buildApplicationMessage(currentApplication, serverAndContainer, modulesAndContainers);
        return "Terminated";
    }
    
    public ApplicationResource getApplication(String applicationName) {
        try {
            Optional<ApplicationResource> application = listAllApps().getContent().stream()
                .filter(a -> a.getName().equals(applicationName))
                .findAny();
            
            if (!application.isPresent()) {
                throw new CloudUnitCliException(MessageFormat.format(NO_SUCH_APPLICATION, applicationName));
            }
            
            return application.get();
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't get application", e);
        }
    }
    
    public ApplicationResource refreshApplication(ApplicationResource application) {
        String url = application.getId().getHref();
        try {
            String result = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            return objectMapper.readValue(result, ApplicationResource.class);
        } catch (IOException | ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't get application", e);
        }
    }
    
    public ApplicationResource getSpecificOrCurrentApplication(String applicationName) {
        authenticationUtils.checkConnected();
        fileUtils.checkNotInFileExplorer();
        
        if (StringUtils.isEmpty(applicationName)) {
            checkApplicationSelected();
            return currentApplication;
        } else {
            checkApplicationExists(applicationName);
            return getApplication(applicationName);
        }
    }

    public String useApplication(String applicationName) {
        authenticationUtils.checkConnected();
        fileUtils.checkNotInFileExplorer();

        currentApplication = getApplication(applicationName);
        return MessageFormat.format("Using application \"{0}\"", currentApplication.getName());
    }
    
    public String createApp(String applicationName, String serverName) {
        authenticationUtils.checkConnected();
        fileUtils.checkNotInFileExplorer();

        try {
            checkUtils.checkImageExists(serverName);
            String url = String.format(APPLICATIONS_URL_FORMAT, authenticationUtils.finalHost);
            
            ApplicationResource request = new ApplicationResource();
            request.setName(applicationName);
            request.setServerType(serverName);

            String response = (String) restUtils.sendPostCommand(url, authenticationUtils.getMap(), request);
            
            try {
                currentApplication = objectMapper.readValue(response, ApplicationResource.class);
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return MessageFormat.format(APPLICATION_CREATED, currentApplication.getName());
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't create application", e);
        }
    }

    public String rmApp(String applicationName, boolean errorIfNotExists, Prompter prompter) {
        authenticationUtils.checkConnected();
        fileUtils.checkNotInFileExplorer();
        
        ApplicationResource application = null;
        
        if (StringUtils.isEmpty(applicationName)) {
            checkApplicationSelected();
            
            application = currentApplication;
        } else {
            if (!errorIfNotExists && !applicationExists(applicationName)) {
                return MessageFormat.format(NO_SUCH_APPLICATION, applicationName);
            }
            
            application = getApplication(applicationName);
        }
        
        if (prompter != null) {
            boolean confirmed = prompter.promptConfirmation(MessageFormat.format("Remove application \"{0}\"?",
                    application.getName()));
            if (!confirmed) {
                return "Abort";
            }
        }
        
        try {
            String url = application.getId().getHref();
            restUtils.sendDeleteCommand(url, authenticationUtils.getMap());
            
            if (application.equals(currentApplication)) {
                currentApplication = null;
            }
            
            return MessageFormat.format(APPLICATION_REMOVED, application.getName());
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't remove application", e);
        }
    }

    public String startApp(String applicationName) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);

        if (!application.hasLink("start")) {
            throw new CloudUnitCliException(MessageFormat.format("Cannot start application when in state {0}.",
                    application.getStatus()));
        }
        String url = application.getLink("start").getHref();
        try {
            restUtils.sendPostCommand(url, authenticationUtils.getMap(), new HashMap<>());
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't start application", e);
        }
        
        return MessageFormat.format(APPLICATION_STARTED, application.getName());
    }

    public String stopApp(String applicationName) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);

        if (!application.hasLink("stop")) {
            throw new CloudUnitCliException(MessageFormat.format("Cannot stop application when in state {0}.",
                    application.getStatus()));
        }
        String url = application.getLink("stop").getHref();
        try {
            restUtils.sendPostCommand(url, authenticationUtils.getMap(), new HashMap<>());
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't stop application", e);
        }
        
        return MessageFormat.format(APPLICATION_STOPPED, application.getName());
    }

    public Resources<ApplicationResource> listAllApps() throws ManagerResponseException {
        String url = String.format(APPLICATIONS_URL_FORMAT, authenticationUtils.finalHost);
        String response = null;
        try {
            response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't list applications", e);
        }
        
        Resources<ApplicationResource> applications;
        try {
            applications = objectMapper.readValue(response, new TypeReference<Resources<ApplicationResource>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return applications;
    }

    public String listAll() {
        authenticationUtils.checkConnected();
        fileUtils.checkNotInFileExplorer();

        List<ApplicationResource> applications = null;
        try {
            applications = new ArrayList<>(listAllApps().getContent());
        } catch (ManagerResponseException e) {
            return ANSIConstants.ANSI_RED + e.getMessage() + ANSIConstants.ANSI_RESET;
        }
        if (applications != null) {
            MessageConverter.buildListApplications(applications);
        }
        return MessageFormat.format(APPLICATION_COUNT, applications.size());
    }

    public String deployFromAWar(File path, String contextPath, boolean openBrowser) {
        checkConnectedAndApplicationSelected();

        Guard.guardTrue(path != null, "Please specify a file path");
        
        String url = currentApplication.getLink("deployments").getHref();
        
        File file = path;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.available();
            fileInputStream.close();
        } catch (IOException e) {
            throw new CloudUnitCliException("The file could not be opened", e);
        }
        
        FileSystemResource resource = new FileSystemResource(file);
        Map<String, Object> params = new HashMap<>();
        params.put("file", resource);
        params.put("contextPath", contextPath);
        params.putAll(authenticationUtils.getMap());
        
        String body = (String) restUtils.sendPostForUpload(url, params).get("body");
        
        DeploymentResource deployment;
        try {
            deployment = objectMapper.readValue(body, DeploymentResource.class);
        } catch (IOException e) {
            throw new CloudUnitCliException("Couldn't deploy application", e);
        }

        String deploymentUrl = deployment.getLink("open").getHref();
        
        if (openBrowser) {
            DesktopAPI.browse(URI.create(deploymentUrl));
        }

        return MessageFormat.format("Application deployed. Access at {0}", deploymentUrl);
    }

    public String addNewAlias(String applicationName, String aliasName) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);

        String url = application.getLink("aliases").getHref();
        AliasResource request = new AliasResource(aliasName);
        try {
            String response = restUtils.sendPostCommand(url, authenticationUtils.getMap(), request);
            
            AliasResource alias = objectMapper.readValue(response, AliasResource.class);
            return MessageFormat.format(ALIAS_ADDED,
                    alias.getName(),
                    application.getName());
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't add new alias", e);
        }
    }

    public String listAllAliases(String applicationName) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);

        Resources<AliasResource> aliases = getAliases(application);

        MessageConverter.buildListAliases(aliases.getContent());

        return MessageFormat.format(ALIAS_COUNT, aliases.getContent().size());
    }
    
    public Resources<ContainerResource> getContainers(ApplicationResource application) {
        String url = application.getLink("containers").getHref();
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            
            Resources<ContainerResource> aliases = objectMapper.readValue(response, new TypeReference<Resources<ContainerResource>>() {});
            return aliases;
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get containers", e);
        }
    }
    
    public ContainerResource getContainer(ApplicationResource application, String containerName) {
        return getContainers(application).getContent().stream()
                .filter(c -> c.getName().equals(containerName))
                .findAny()
                .orElseThrow(() -> new CloudUnitCliException(MessageFormat.format(NO_SUCH_CONTAINER,
                        containerName,
                        getAvailableContainerNames())));
    }
    
    public ServerResource getServer(ApplicationResource application) {
        String url = application.getLink("server").getHref();
        
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            
            return objectMapper.readValue(response, ServerResource.class);
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get server", e);
        }
    }

    public ContainerResource getServerContainer(ServerResource server) {
        String url = server.getLink("container").getHref();
        
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            
            return objectMapper.readValue(response, ContainerResource.class);
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get server container", e);
        }
    }

    public Resources<AliasResource> getAliases(ApplicationResource application) {
        String url = application.getLink("aliases").getHref();
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            
            Resources<AliasResource> aliases = objectMapper.readValue(response, new TypeReference<Resources<AliasResource>>() {});
            return aliases;
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get aliases", e);
        }
    }
    
    public AliasResource getAlias(ApplicationResource application, String aliasName) {
        return getAliases(application).getContent().stream()
            .filter(a -> a.getName().equals(aliasName))
            .findAny()
            .orElseThrow(() -> new CloudUnitCliException(MessageFormat.format(NO_SUCH_ALIAS, aliasName)));
    }

    public String removeAlias(String applicationName, String aliasName) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);
        
        AliasResource alias = getAlias(application, aliasName);
        
        String url = alias.getId().getHref();
        try {
            restUtils.sendDeleteCommand(url, authenticationUtils.getMap());
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't remove alias");
        }

        statusCommand.setExitStatut(0);

        return MessageFormat.format(ALIAS_REMOVED, alias.getName(), application.getName());
    }

    @Deprecated
    public String checkAndRejectIfError(String applicationName) {
        if (authenticationUtils.isConnected()) {
            return ANSIConstants.ANSI_RED + "You are not connected to CloudUnit host! Please use connect command"
                    + ANSIConstants.ANSI_RESET;
        }

        if (fileUtils.isInFileExplorer()) {
            statusCommand.setExitStatut(1);
            return ANSIConstants.ANSI_RED
                    + "You are currently in a container file explorer. Please exit it with close-explorer command"
                    + ANSIConstants.ANSI_RESET;
        }

        if (currentApplication == null && applicationName == null) {
            statusCommand.setExitStatut(1);
            return ANSIConstants.ANSI_RED
                    + "No application is currently selected by the following command line : use <application name>"
                    + ANSIConstants.ANSI_RESET;

        }
        
        if (applicationName != null) {
            log.log(Level.INFO, applicationName);
            return useApplication(applicationName);
        }

        return null;
    }
    
    public String createEnvironmentVariable(String applicationName, String key, String value) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);
        
        Guard.guardTrue(StringUtils.isNotEmpty(key), "No key was given");
        Guard.guardTrue(Pattern.matches("^[_A-Z][_A-Z0-9]*$", key), "Invalid key name \"{0}\"", key);
        
        EnvironmentVariableResource request = new EnvironmentVariableResource(key, value);
        
        ContainerResource serverContainer = getServerContainer(getServer(application));
        
        String url = serverContainer.getLink("env-vars").getHref();
        try {
            restUtils.sendPostCommand(url, authenticationUtils.getMap(), request);
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't create environment variable", e);
        }

        return MessageFormat.format(ENV_VAR_ADDED, key, application.getName());
    }
    
    public Resources<EnvironmentVariableResource> getEnvironmentVariables(ContainerResource container) {
        String url = container.getLink("env-vars").getHref();
        
        try {
            String response = restUtils.sendGetCommand(url, authenticationUtils.getMap()).get("body");
            
            return objectMapper.readValue(response, new TypeReference<Resources<EnvironmentVariableResource>>() {});
        } catch (ManagerResponseException | IOException e) {
            throw new CloudUnitCliException("Couldn't get environment variables");
        }
    }

    public EnvironmentVariableResource getEnvironmentVariable(ContainerResource container, String key) {
        return getEnvironmentVariables(container).getContent().stream()
                .filter(var -> var.getName().equals(key))
                .findAny()
                .orElseThrow(() -> new CloudUnitCliException(MessageFormat.format(NO_SUCH_ENV_VAR, key)));
    }

    public String removeEnvironmentVariable(String applicationName, String key) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);
        ServerResource server = getServer(application);
        ContainerResource serverContainer = getServerContainer(server);
        EnvironmentVariableResource variable = getEnvironmentVariable(serverContainer, key);
        
        String url = variable.getId().getHref();
        try {
            restUtils.sendDeleteCommand(url, authenticationUtils.getMap());
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't remove environment variable", e);
        }

        return MessageFormat.format(ENV_VAR_REMOVED, variable.getName(), application.getName());
    }

    public String updateEnvironmentVariable(String applicationName, String key, String value) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);
        ServerResource server = getServer(application);
        ContainerResource serverContainer = getServerContainer(server);
        EnvironmentVariableResource variable = getEnvironmentVariable(serverContainer, key);
        
        String url = variable.getId().getHref();
        try {
            variable.setValue(value);

            restUtils.sendPutCommand(url, authenticationUtils.getMap(), variable);
        } catch (ManagerResponseException e) {
            throw new CloudUnitCliException("Couldn't update environment variable", e); 
        }

        return "OK";
    }

    public String listAllEnvironmentVariables(String applicationName) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);
        ServerResource server = getServer(application);
        ContainerResource serverContainer = getServerContainer(server);
        Resources<EnvironmentVariableResource> variables = getEnvironmentVariables(serverContainer);
        
        MessageConverter.buildListEnvironmentVariables(variables.getContent());

        return MessageFormat.format(ENV_VARS_COUNT, variables.getContent().size());
    }

    public String listContainers(String applicationName) {
        ApplicationResource application = getSpecificOrCurrentApplication(applicationName);
        ServerResource server = getServer(currentApplication);
        ContainerResource serverContainer = getServerContainer(server);
        
        List<String> containers = new ArrayList<>();
        containers.add(serverContainer.getName());

        for (ModuleResource module : moduleUtils.getModules(application)) {
            containers.add(module.getName());
        }
        MessageConverter.buildListContainers(containers);

        return containers.size() + " containers found!";
    }

    public String listCommands(String containerName) {
        String response;

        ServerResource server = getServer(currentApplication);
        ContainerResource serverContainer = getServerContainer(server);
        try {
            response = restUtils.sendGetCommand(
                    authenticationUtils.finalHost + urlLoader.actionApplication + currentApplication.getName()
                            + "/container/" + serverContainer.getName() + "/command",
                    authenticationUtils.getMap()).get("body");

        } catch (ManagerResponseException e) {
            statusCommand.setExitStatut(1);
            return ANSIConstants.ANSI_RED + e.getMessage() + ANSIConstants.ANSI_RESET;
        }

        MessageConverter.buildListCommands(JsonConverter.getCommands(response));

        statusCommand.setExitStatut(0);

        return JsonConverter.getCommands(response).size() + " commands found!";
    }

    public String execCommand(String name, String containerName, String arguments) {
        if (containerName == null) {
            if (getCurrentApplication() == null) {
                statusCommand.setExitStatut(1);
                return ANSIConstants.ANSI_RED
                        + "No application is currently selected by the following command line : use <application name>"
                        + ANSIConstants.ANSI_RESET;
            }
            ServerResource server = getServer(currentApplication);
            ContainerResource serverContainer = getServerContainer(server);

            containerName = serverContainer.getName();
        }

        try {
            Command command = new Command();
            command.setName(name);
            command.setArguments(Arrays.asList(arguments.split(",")));
            ObjectMapper objectMapper = new ObjectMapper();
            String entity = objectMapper.writeValueAsString(command);
            restUtils.sendPostCommand(
                    authenticationUtils.finalHost + urlLoader.actionApplication + currentApplication.getName()
                            + "/container/" + containerName + "/command/" + name + "/exec",
                    authenticationUtils.getMap(), entity);
        } catch (ManagerResponseException | JsonProcessingException e) {
            statusCommand.setExitStatut(1);
            return ANSIConstants.ANSI_RED + e.getMessage() + ANSIConstants.ANSI_RESET;
        }
        statusCommand.setExitStatut(0);

        return "The command " + name + " has been executed";
    }

    public String getAvailableContainerNames() {
        return String.join("\t", getContainers(currentApplication).getContent().stream()
                .map(c -> c.getName())
                .toArray(String[]::new));
    }
}
