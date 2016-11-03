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

package fr.treeptik.cloudunit.security;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Random;

import javax.inject.Inject;
import javax.servlet.Filter;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import fr.treeptik.cloudunit.dto.AliasResource;
import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.ServerResource;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.initializer.CloudUnitApplicationContext;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.UserService;
import fr.treeptik.cloudunit.test.ApplicationTemplate;

/**
 * This scenario is to verify the protection about resouces between users
 * If an UserA creates an application, UserB should not modify it.
 * We tests between the profils the security for each route.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        CloudUnitApplicationContext.class,
        MockServletContext.class
})
@ActiveProfiles("integration")
@DirtiesContext
public class SecurityTestIT {
    private static final String SERVER_TYPE = "tomcat-8";

    private final Logger logger = LoggerFactory.getLogger(SecurityTestIT.class);

    @Inject
    private WebApplicationContext context;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private Filter springSecurityFilterChain;

    @Inject
    private UserService userService;
    
    @Value("${suffix.cloudunit.io}")
    private String domain;

    private MockMvc mockMvc;

    private MockHttpSession session1;
    private MockHttpSession session2;
    
    private ApplicationTemplate applicationTemplate1;
    private ApplicationTemplate applicationTemplate2;

    private static String applicationName;
    
    private ApplicationResource application;

    // Persist the context for user1
    private User user1 = null;

    @BeforeClass
    public static void initEnv() {
        applicationName = "App"+new Random().nextInt(100000);
    }

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();

        // If user1 is null (first test) we create its session and its application
        try {
            logger.info("Create session for user1 : " + user1);
            // we affect the user to skip this branch too
            User user1 = userService.findByLogin("usertest1");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user1.getLogin(), user1.getPassword());
            Authentication result = authenticationManager.authenticate(authentication);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(result);
            session1 = new MockHttpSession();
            session1.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        } catch (ServiceException e) {
            logger.error(e.getLocalizedMessage());
        }

        // After the first tests, all others are for User2
        try {
            logger.info("Create session for user2");
            User user2 = userService.findByLogin("usertest2");
            Authentication authentication = new UsernamePasswordAuthenticationToken(user2.getLogin(), user2.getPassword());
            Authentication result = authenticationManager.authenticate(authentication);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(result);
            session2 = new MockHttpSession();
            session2.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        } catch (ServiceException e) {
            logger.error(e.getLocalizedMessage());
        }
        
        applicationTemplate1 = new ApplicationTemplate(mockMvc, session1);
        applicationTemplate2 = new ApplicationTemplate(mockMvc, session2);
        
        application = applicationTemplate1.createAndAssumeApplication(applicationName, SERVER_TYPE);
    }

    @After
    public void tearDown() throws Exception {
        applicationTemplate1.removeApplication(application);
        
        SecurityContextHolder.clearContext();
        session1.invalidate();
        session2.invalidate();
    }

    @Test
    public void test_koUser2StopsUser1Application() throws Exception {
        applicationTemplate2.stopApplication(application)
            .andExpect(status().isForbidden());
    }
    
    @Test
    public void test_koUser2StartsUser1Application() throws Exception {
        applicationTemplate1.stopApplication(application)
            .andExpect(status().isNoContent());
        
        application = applicationTemplate1.refreshApplication(application);
        
        applicationTemplate2.startApplication(application)
            .andExpect(status().isForbidden());
    }
    
    @Test
    public void test_koUser2RemovesUser1Application() throws Exception {
        applicationTemplate2.removeApplication(application)
            .andExpect(status().isForbidden());
    }

    @Test
    public void test_koUser2CreatesAliasForUser1Application() throws Exception {
        applicationTemplate2.addAlias(application, "myalias" + domain)
            .andExpect(status().isForbidden());
    }
    
    @Test
    public void test_koUser2RemovesAliasForUser1Application() throws Exception {
        ResultActions result = applicationTemplate1.addAlias(application, "myalias" + domain);
        result.andExpect(status().isCreated());
        AliasResource alias = applicationTemplate1.getAlias(result);
        
        applicationTemplate2.removeAlias(alias)
            .andExpect(status().isForbidden());
    }

    @Test
    public void test_User2ChangesConfigForUser1Application() throws Exception {
        ServerResource server = applicationTemplate1.getServer(application);
        
        applicationTemplate2.setJvmMemory(server, 512L)
            .andExpect(status().isForbidden());
    }

}