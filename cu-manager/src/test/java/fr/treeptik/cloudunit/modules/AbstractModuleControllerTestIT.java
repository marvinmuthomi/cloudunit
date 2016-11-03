/*
 * LICENCE : CloudUnit is available under the Affero Gnu Public License GPL V3 : https://www.gnu.org/licenses/agpl-3.0.html
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

package fr.treeptik.cloudunit.modules;

import static org.awaitility.Awaitility.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.Filter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resources;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.mongodb.MongoClient;

import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.EnvironmentVariableResource;
import fr.treeptik.cloudunit.dto.ModulePortResource;
import fr.treeptik.cloudunit.dto.ModuleResource;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.initializer.CloudUnitApplicationContext;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.UserService;
import fr.treeptik.cloudunit.test.ApplicationTemplate;
import fr.treeptik.cloudunit.test.ContainerTemplate;
import fr.treeptik.cloudunit.test.ModuleTemplate;
import fr.treeptik.cloudunit.utils.CheckBrokerConnectionUtils;
import fr.treeptik.cloudunit.utils.TestUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 * Tests for Module lifecycle
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    CloudUnitApplicationContext.class,
    MockServletContext.class
})
@ActiveProfiles("integration")
public abstract class AbstractModuleControllerTestIT {
    protected final Logger logger = LoggerFactory.getLogger(AbstractModuleControllerTestIT.class);

    @Inject
    protected WebApplicationContext context;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private Filter springSecurityFilterChain;

    @Inject
    private UserService userService;

    @Value("${cloudunit.instance.name}")
    private String cuInstanceName;

    @Value("${ip.box.vagrant}")
    protected String ipVagrantBox;

    protected MockMvc mockMvc;

    protected MockHttpSession session;
    
    protected ApplicationTemplate applicationTemplate;
    protected ModuleTemplate moduleTemplate;
    protected ContainerTemplate containerTemplate;

    protected String applicationName;
    protected ApplicationResource application;
    protected ModuleResource module;

    @Inject
    private CheckBrokerConnectionUtils checkBrokerConnectionUtils;

    protected String serverType;
    protected String moduleName;
    protected String numberPort;
    protected String managerPrefix;
    protected String managerSuffix;
    protected String managerPageContent;
    protected String testScriptPath;

    @Before
    public void setUp() throws Exception {
        applicationName = "app" + new Random().nextInt(100000);

        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();

        User user = null;
        try {
            user = userService.findByLogin("johndoe");
        } catch (ServiceException e) {
            logger.error(e.getLocalizedMessage());
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getLogin(), user.getPassword());
        Authentication result = authenticationManager.authenticate(authentication);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(result);
        session = new MockHttpSession();
        String secContextAttr = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
        session.setAttribute(secContextAttr, securityContext);
        
        applicationTemplate = new ApplicationTemplate(mockMvc, session);
        moduleTemplate = new ModuleTemplate(mockMvc, session);
        containerTemplate = new ContainerTemplate(mockMvc, session);

        // create an application server
        application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
    }

    @After
    public void tearDown() throws Exception {
        applicationTemplate.removeApplication(application)
            .andExpect(status().isNoContent());

        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Test
    public void test_failToAddModuleBecauseModuleEmpty() throws Exception {
        applicationTemplate.addModule(application, "")
            .andExpect(status().isBadRequest());
    }

    @Test
    public void test_failToAddModuleBecauseModuleNonExisting() throws Exception {
        applicationTemplate.addModule(application, "dzqmokdzq")
            .andExpect(status().isBadRequest());
    }

    @Test
    public void test_addModule() throws Exception {
        ResultActions result = applicationTemplate.addModule(application, moduleName);
        result.andExpect(status().isCreated());
        
        Resources<ModuleResource> modules = applicationTemplate.getModules(application);
        
        assertThat(modules.getContent(), contains(hasProperty("name", equalTo(moduleName))));
    }
    
    @Test
    public void test_addThenRemoveModule() throws Exception {
        ResultActions result = applicationTemplate.addModule(application, moduleName);
        result.andExpect(status().isCreated());        
        module = applicationTemplate.getModule(result);
        
        Resources<ModuleResource> modules = applicationTemplate.getModules(application);
        
        assumeThat(modules.getContent(), contains(hasProperty("name", equalTo(moduleName))));
        
        result = applicationTemplate.removeModule(module);
        result.andExpect(status().isNoContent());
        
        modules = applicationTemplate.getModules(application);
        
        assertThat(modules.getContent(), empty());
    }


    @Test
    public void test_addModuleTwice() throws Exception {
        ResultActions result = applicationTemplate.addModule(application, moduleName);
        result.andExpect(status().isCreated());

        result = applicationTemplate.addModule(application, moduleName);
        result.andExpect(status().isBadRequest());
        
        Resources<ModuleResource> modules = applicationTemplate.getModules(application);
        
        assertThat(modules.getContent(), contains(hasProperty("name", equalTo(moduleName))));
    }

    @Test
    public void test_addModuleThenRestart() throws Exception {
        ResultActions result = applicationTemplate.addModule(application, moduleName);
        result.andExpect(status().isCreated());
        
        applicationTemplate.stopApplication(application)
            .andExpect(status().isNoContent());

        application = applicationTemplate.refreshApplication(application);

        applicationTemplate.startApplication(application)
            .andExpect(status().isNoContent());
        
        Resources<ModuleResource> modules = applicationTemplate.getModules(application);
        
        assertThat(modules.getContent(), contains(hasProperty("name", equalTo(moduleName))));
    }

    @Test
    public void test_PublishPort() throws Exception {
        ResultActions result = requestAddModule()
        	.andExpect(status().isCreated());
        
        module = applicationTemplate.getModule(result);
        
        Resources<ModulePortResource> ports = moduleTemplate.getPorts(module);
        
        Optional<ModulePortResource> maybePort = ports.getContent().stream()
                .filter(p -> p.getNumber().equals(numberPort))
                .findAny();
        
        assumeTrue("Module has port", maybePort.isPresent());
        
        result = moduleTemplate.publishPort(maybePort.get());
        result.andExpect(status().isOk());
        ModulePortResource port = moduleTemplate.getPort(result);

        checkConnection(port.getHostNumber());
    }

    protected abstract void checkConnection(String forwardedPort) throws Exception;

    @Test
    public void test_runScript() throws Exception {
        assumeNotNull(testScriptPath);
        
        ResultActions result = applicationTemplate.addModule(application, moduleName);
        result.andExpect(status().isCreated());
        module = applicationTemplate.getModule(result);
        
        result = applicationTemplate.runScript(module, testScriptPath);
        result.andExpect(status().isOk());
    }
    
    private String getEnvironmentValue(Resources<EnvironmentVariableResource> environment, String variableName) {
        Optional<String> value = environment.getContent().stream()
            .filter(e -> e.getName().equals(variableName))
            .findAny()
            .map(e -> e.getValue());
        assertTrue(String.format("Variable %s present", variableName), value.isPresent());
        String user = value.get();
        return user;
    }

    /**
     * Inner class to check a relational database connection
     *
     */
    public class CheckDatabaseConnection {

        public void invoke(String forwardedPort, String keyUser, String keyPassword,
                           String keyDB, String driver, String jdbcUrlPrefix) throws Exception {
            ContainerResource moduleContainer = moduleTemplate.getContainer(module);
            Resources<EnvironmentVariableResource> environment = containerTemplate.getFullEnvironment(moduleContainer);
            
            String user = getEnvironmentValue(environment, keyUser);
            String password = getEnvironmentValue(environment, keyPassword);
            String database = getEnvironmentValue(environment, keyDB);
            String jdbcUrl = jdbcUrlPrefix+ipVagrantBox+":"+forwardedPort+"/" + database;
            Class.forName(driver);
            await("Testing database connection...").atMost(5, TimeUnit.SECONDS)
                   .and().ignoreExceptions()
                   .until(() -> {
                       try(final Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
                           return connection.isValid(1000);
                       }
                   });
        }
    }

    /**
     * Inner class to check a message broker connection
     *
     */
    public class CheckBrokerConnection {
        public void invoke(String forwardedPort, String keyUser, String keyPassword, String keyDB, String protocol)
                throws Exception {
            ContainerResource moduleContainer = moduleTemplate.getContainer(module);
            Resources<EnvironmentVariableResource> environment = containerTemplate.getFullEnvironment(moduleContainer);
            
            String user = getEnvironmentValue(environment, keyUser);
            String password = getEnvironmentValue(environment, keyPassword);
            String vhost = getEnvironmentValue(environment, keyDB);
            String brokerURL = ipVagrantBox+":"+forwardedPort;
            String message = "Hello world!";
            String actualMessage = null;

            switch(protocol){
                case "JMS" :
                    actualMessage = checkBrokerConnectionUtils.checkActiveMQJMSProtocol(message, brokerURL);
                    break;
                case "AMQP" :
                    actualMessage = checkBrokerConnectionUtils.checkRabbitMQAMQPProtocol(message, brokerURL, user, password, vhost);
                    break;
                default:
                    throw new RuntimeException("Protocol " + keyDB + " not supported yet");
            }
            
            assertEquals(message, actualMessage);
        }
    }

    /**
     * Inner class to check ElasticSearch connection
     *
     */
    public class CheckElasticSearchConnection {
        public void invoke(String forwardedPort) {
            String url = String.format("http://%s:%s", ipVagrantBox, forwardedPort);

            await("Testing database connection...").atMost(5, TimeUnit.SECONDS)
            .and().ignoreExceptions()
            .until(() -> TestUtils.getUrlContentPage(url).contains("elasticsearch"));
        }
    }

    /**
     * Inner class to check Redis connection
     *
     */
    public class CheckRedisConnection {
        public void invoke(String forwardedPort) {
            try(JedisPool pool = new JedisPool(
                    new JedisPoolConfig(), ipVagrantBox, Integer.parseInt(forwardedPort), 3000)){
                pool.getResource();
            }
        }
    }

    /**
     * Inner class to check MongoDB connection
     *
     */
    public class CheckMongoConnection {
        public void invoke(String forwardedPort) throws Exception {
            int port = Integer.parseInt(forwardedPort);
            MongoClient mongo = new MongoClient(ipVagrantBox, port);
            mongo.close();
        }
    }

    private ResultActions requestAddModule() throws Exception {
        return applicationTemplate.addModule(application, moduleName);
    }
}