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

package fr.treeptik.cloudunit.ports;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
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
import org.springframework.hateoas.Link;
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

import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.PortResource;
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
@ContextConfiguration(classes = {
        CloudUnitApplicationContext.class,
        MockServletContext.class
})
@ActiveProfiles("integration")
public class PortsControllerTestIT {
    private static final int PORT_NUMBER = 6115;
    
    private final Logger logger = LoggerFactory.getLogger(PortsControllerTestIT.class);

    private final String serverType = "tomcat-8";

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

    private String applicationName;

    private ApplicationResource application;

    @Before
    public void setUp() throws Exception {
        applicationName = "App" + new Random().nextInt(10000);
        
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
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        
        applicationTemplate = new ApplicationTemplate(mockMvc, session);

        application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
    }

    @After
    public void tearDown() throws Exception {
        applicationTemplate.removeApplication(application);

        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Test
    public void test_okOpenAndClosePort() throws Exception {
        // Add port
        ResultActions result = applicationTemplate.openPort(application, PORT_NUMBER, "web");
        result.andExpect(status().isCreated());
        PortResource port = applicationTemplate.getPort(result);

        Resources<PortResource> ports = applicationTemplate.getPorts(application);

        assertThat(ports.getContent(), contains(hasProperty("number", equalTo(PORT_NUMBER))));
        
        // Remove port
        result = applicationTemplate.closePort(port);
        result.andExpect(status().isNoContent());

        ports = applicationTemplate.getPorts(application);

        assertThat(ports.getContent(), empty());
    }

    @Test
    public void test_koAddPortNegativeNumber() throws Exception {
        applicationTemplate.openPort(application, -1, "web")
            .andExpect(status().isBadRequest());
    }
    
    @Test
    public void test_koAddPortZeroNumber() throws Exception {
        applicationTemplate.openPort(application, 0, "web")
            .andExpect(status().isBadRequest());
    }
    
    @Test
    public void test_koAddPortNoNumber() throws Exception {
        applicationTemplate.openPort(application, null, "web")
            .andExpect(status().isBadRequest());
    }

    @Test
    public void test_koAddPortUnknownNature() throws Exception {
        applicationTemplate.openPort(application, PORT_NUMBER, "dzqmok")
            .andExpect(status().isBadRequest());
    }

    @Test
    public void test_koClosePortUnknown() throws Exception {
        PortResource port = new PortResource();
        port.add(new Link(application.getId().getHref() + "/ports/" + PORT_NUMBER));
        
        applicationTemplate.closePort(port)
            .andExpect(status().isNotFound());
    }
}