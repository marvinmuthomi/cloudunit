package fr.treeptik.cloudunit.deployments;

import static fr.treeptik.cloudunit.utils.TestUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import fr.treeptik.cloudunit.dto.PortResource;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.initializer.CloudUnitApplicationContext;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.UserService;
import fr.treeptik.cloudunit.test.ApplicationTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        CloudUnitApplicationContext.class,
        MockServletContext.class
        })
@ActiveProfiles("integration")
public abstract class AbstractFatjarDeploymentControllerIT {
    private static final String SPRING_BOOT_MYSQL_WAR_URL = "https://github.com/Treeptik/CloudUnit/releases/download/1.0/spring-boot-mysql.jar";
    private static final String SPRING_BOOT_WAR_URL = "https://github.com/Treeptik/CloudUnit/releases/download/1.0/spring-boot.jar";

    private final Logger logger = LoggerFactory.getLogger(AbstractFatjarDeploymentControllerIT.class);

    @Inject
    private WebApplicationContext context;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private Filter springSecurityFilterChain;

    @Inject
    private UserService userService;

    private MockMvc mockMvc;
    private MockHttpSession session;
    
    private ApplicationTemplate applicationTemplate;

    protected final String serverType = "fatjar";
    private String applicationName;

    @Before
    public void setUp() {
        applicationName = "App" + new Random().nextInt(100000);
        
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
        
        applicationTemplate = new ApplicationTemplate(mockMvc, session);
    }

    @Ignore
    @Test
    public void test_deploySimpleApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            ResultActions result = applicationTemplate.openPort(application, 8080, "web");
            result.andExpect(status().isCreated());
            
            PortResource port = applicationTemplate.getPort(result);
    
            result = applicationTemplate.addDeployment(application, "ROOT", SPRING_BOOT_WAR_URL);
            result.andExpect(status().isCreated());
                    
            String url = port.getLink("open").getHref();
            
            String expectedUrl = String.format("http://%s-johndoe-forward-8080.cloudunit.dev",
                    applicationName.toLowerCase());
            assertEquals(expectedUrl, url);
            
            Optional<String> content = waitForContent(url);
            assertTrue("Got content", content.isPresent());
            assertThat(content.get(), containsString("Greetings from Spring Boot!"));
        } finally {
            applicationTemplate.removeApplication(application);
        }
    }

    @Ignore
    @Test
    public void test_deployMysqlApplicationTest() throws Exception {
        ApplicationResource application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
        try {
            ResultActions result = applicationTemplate.openPort(application, 8080, "web");
            result.andExpect(status().isCreated());
            PortResource port = applicationTemplate.getPort(result);
            
            applicationTemplate.addModule(application, "mysql-5-5")
                .andExpect(status().isCreated());
            
            applicationTemplate.addDeployment(application, "ROOT", SPRING_BOOT_MYSQL_WAR_URL)
                .andExpect(status().isCreated());
    
            String url = port.getLink("open").getHref();
            
            String expectedUrl = String.format("http://%s-johndoe-forward-8080.cloudunit.dev",
                    applicationName.toLowerCase());
            assertEquals(expectedUrl, url);
    
            // Wait for the deployment
            Optional<String> content = waitForContent(url);
            assertTrue("Got content", content.isPresent());
            assertThat(content.get(), containsString("CloudUnit PaaS"));
        
            String addUserUrl = url + "/create?email=johndoe@gmail.com&name=johndoe";
    
            result = mockMvc.perform(get(addUserUrl).session(session));
            result.andExpect(status().isOk());
            String addUserContent = getUrlContentPage(addUserUrl);
            
            assertThat(addUserContent, containsString("User succesfully created!"));
        } finally {
            applicationTemplate.removeApplication(application);
        }
    }
}
