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

package fr.treeptik.cloudunit.servers;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Random;

import javax.inject.Inject;
import javax.json.Json;
import javax.servlet.Filter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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

import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.ServerResource;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.initializer.CloudUnitApplicationContext;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.UserService;
import fr.treeptik.cloudunit.test.ApplicationTemplate;

/**
 * Created by nicolas on 08/09/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {CloudUnitApplicationContext.class, MockServletContext.class})
@ActiveProfiles("integration")
public abstract class AbstractApplicationControllerTestIT {
    private final Logger logger = LoggerFactory.getLogger(AbstractApplicationControllerTestIT.class);

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private UserService userService;
    
    @Inject
    private Filter springSecurityFilterChain;
    
    @Inject
    private WebApplicationContext context;

    private MockMvc mockMvc;
    
    private MockHttpSession session;

    private String applicationName;
    
    private ApplicationTemplate applicationTemplate;

    private final String serverType;

    protected AbstractApplicationControllerTestIT(String serverType) {
        this.serverType = serverType;
    }

    @Before
    public void setUp() throws Exception {
        logger.info("setup");
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();

        User user = null;
        try {
            user = userService.findByLogin("johndoe");
        } catch (ServiceException e) {
            logger.error(e.getLocalizedMessage());
        }

        Authentication authentication = null;
        if (user != null) {
            authentication = new UsernamePasswordAuthenticationToken(user.getLogin(), user.getPassword());
        }
        Authentication result = authenticationManager.authenticate(authentication);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(result);
        session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        
        applicationName = "app" + new Random().nextInt(100000);
        
        applicationTemplate = new ApplicationTemplate(mockMvc, session);
    }

    @After
    public void tearDown() throws Exception {
        logger.info("teardown");
        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Test
    public void test_failCreateEmptyNameApplication() throws Exception {
        ResultActions result = applicationTemplate.createApplication("", serverType);
        result.andExpect(status().is4xxClientError());
    }

    @Test(timeout = 30000)
    public void test_failCreateWrongNameApplication() throws Exception {
        ResultActions result = applicationTemplate.createApplication("         ", serverType);
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void test_createAccentNameApplication() throws Exception {
        String displayName = "a-eeioù";
        String expectedName = "a-eeiou";

        ApplicationResource application = applicationTemplate.createAndAssumeApplication(displayName, serverType);
        
        assertEquals(expectedName, application.getName());
        
        applicationTemplate.deleteApplication(application);
    }

    @Test()
    public void test_startStopStartApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);

        applicationTemplate.stopApplication(application)
            .andExpect(status().isNoContent());
        
        application = applicationTemplate.refreshApplication(application);

        applicationTemplate.startApplication(application)
            .andExpect(status().isNoContent());

        applicationTemplate.deleteApplication(application)
            .andExpect(status().isNoContent());
    }

    @Test
    public void test_changeJvmMemorySizeApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            ServerResource server = applicationTemplate.getServer(application);
            
            ResultActions result = applicationTemplate.setJvmMemory(server, 1024L);
            result.andExpect(status().isOk());
            
            server = applicationTemplate.getServer(result);
    
            assertEquals(server.getJvmMemory().longValue(), 1024L);
        } finally {
            applicationTemplate.deleteApplication(application);
        }
    }

    @Test(timeout = 30000)
    public void test_changeInvalidJvmMemorySizeApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            ServerResource server = applicationTemplate.getServer(application);
            
            ResultActions result = applicationTemplate.setJvmMemory(server, 666L);
            result.andExpect(status().isBadRequest());
        } finally {
            applicationTemplate.deleteApplication(application);
        }
    }
    
    @Test(timeout = 30000)
    public void test_changeEmptyJvmMemorySizeApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            ServerResource server = applicationTemplate.getServer(application);
            
            server.setJvmMemory(null);
            
            ResultActions result = applicationTemplate.setServer(server);
            result.andExpect(status().isBadRequest());            
        } finally {
            applicationTemplate.deleteApplication(application);
        }
    }

    @Test(timeout = 60000)
    public void test_changeJvmOptionsApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            ServerResource server = applicationTemplate.getServer(application);
            
            ResultActions result = applicationTemplate.setJvmOptions(server, "-Dkey1=value1");
            result.andExpect(status().isOk());
            server = applicationTemplate.getServer(result);
            
            assertEquals(server.getJvmOptions(), "-Dkey1=value1");
        } finally {
            applicationTemplate.deleteApplication(application);
        }        
    }

    @Test(timeout = 30000)
    public void test_changeFailWithXmsJvmOptionsApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            ServerResource server = applicationTemplate.getServer(application);
            
            ResultActions result = applicationTemplate.setJvmOptions(server, "-Xms=512m");
            result.andExpect(status().isBadRequest());
        } finally {
            applicationTemplate.deleteApplication(application);
        }
    }

    @Test()
    public void test_openAPort() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            logger.info("Open custom ports");
            
            String jsonString = Json.createObjectBuilder()
                    .add("port", "6115")
                    .add("alias", "access6115")
                    .build().toString();
            
            ResultActions resultats = mockMvc.perform(post("/server/ports/open")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonString));
            resultats.andExpect(status().isOk());
            
            logger.info("Close custom ports");
            jsonString =
                    "{\"applicationName\":\"" + applicationName
                            + "\",\"portToOpen\":\"6115\",\"alias\":\"access6115\"}";
            resultats =
                    this.mockMvc.perform(post("/server/ports/close").session(session).contentType(MediaType.APPLICATION_JSON).content(jsonString));
            resultats.andExpect(status().isOk());
        } finally {
            applicationTemplate.deleteApplication(application);
        }
    }

}