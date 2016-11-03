package fr.treeptik.cloudunit.servers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Random;

import javax.inject.Inject;
import javax.servlet.Filter;

import org.junit.After;
import org.junit.Before;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import fr.treeptik.cloudunit.dto.ApplicationResource;
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
public class Apache2ControllerTestIT {
    private final Logger logger = LoggerFactory.getLogger(Apache2ControllerTestIT.class);

    protected String serverType = "apache-2-2";

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private WebApplicationContext context;

    @Inject
    private Filter springSecurityFilterChain;

    @Inject
    private UserService userService;

    private MockMvc mockMvc;

    private MockHttpSession session;

    private String applicationName;

    private ApplicationTemplate applicationTemplate;
    
    private ApplicationResource application;

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
        
        application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
    }

    @After
    public void tearDown() throws Exception {
        applicationTemplate.removeApplication(application)
            .andExpect(status().isNoContent());

        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Test()
    public void test_stopThenStartApplication() throws Exception {
        applicationTemplate.stopApplication(application)
            .andExpect(status().isNoContent());
        
        application = applicationTemplate.refreshApplication(application);
        
        applicationTemplate.startApplication(application)
            .andExpect(status().isNoContent());        
    }
}
