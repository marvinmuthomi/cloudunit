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

package fr.treeptik.cloudunit.logs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Random;

import javax.inject.Inject;
import javax.servlet.Filter;

import org.junit.After;
import org.junit.Assert;
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
public class LogsControllerTestIT {
    private final Logger logger = LoggerFactory.getLogger(LogsControllerTestIT.class);

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
    public void setup() throws Exception {
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
    }

    @After
    public void teardown() throws Exception {
        logger.info("teardown");
        SecurityContextHolder.clearContext();
        session.invalidate();
    }


    /**
     * Gather logs from all Tomcats
     *
     * @throws Exception
     */
    @Test
    public void test_display_logs_from_tomcat() throws Exception {
        display_logs_from_tomcat("tomcat-6");
        display_logs_from_tomcat("tomcat-7");
        display_logs_from_tomcat("tomcat-8");
        display_logs_from_tomcat("tomcat-85");
        display_logs_from_tomcat("tomcat-9");
    }

    /**
     * List Files from all Tomcats
     *
     * @throws Exception
     */
    @Test
    public void test_list_files_from_tomcat() throws Exception {
        list_files_from_tomcat("tomcat-6");
        list_files_from_tomcat("tomcat-7");
        list_files_from_tomcat("tomcat-8");
        list_files_from_tomcat("tomcat-85");
        list_files_from_tomcat("tomcat-9");
    }

    /**
     * Gather logs from all Apache
     *
     * @throws Exception
     */
    @Test
    public void test_display_logs_from_apache() throws Exception {
        display_logs_from_apache("apache-2-2");
    }

    private void display_logs_from_apache(String release) throws Exception {
        createApplication(release);
        try {
            gatherAndCheckLogs(release, "stdout", "Apache");
        } finally {
            deleteApplication();
        }
    }

    private void display_logs_from_tomcat(String release) throws Exception {
        String fileToGather = "catalina.log";
        String keyWord = "Catalina";
        createApplication(release);
        try {
            gatherAndCheckLogs(release, fileToGather, keyWord);
        } finally {
            deleteApplication();
        }
    }

    private void list_files_from_tomcat(String release) throws Exception {
        String fileToCheck = "catalina.log";
        createApplication(release);
        try {
            list_files_and_check_presence(release, fileToCheck);
        } finally {
            deleteApplication();
        }
    }

    private void gatherAndCheckLogs(String release, String fileToGather, String keyWord) throws Exception {
        String container = "int-johndoe-"+applicationName+"-"+release;
        String url = "/logs/" + applicationName + "/container/" + container + "/source/"+fileToGather+"/rows/10";
        ResultActions resultActions =
                mockMvc.perform(get(url).session(session).contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isOk());
        String contentAsString = resultActions.andReturn().getResponse().getContentAsString();
        logger.info(contentAsString);
        Assert.assertTrue(contentAsString.contains(keyWord));
    }

    private void deleteApplication() throws Exception {
        applicationTemplate.removeApplication(application);
    }

    private void createApplication(String serverType) throws Exception {
        application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
    }

    private void list_files_and_check_presence(String release, String fileToCheck) throws Exception {
        String container = "int-johndoe-"+applicationName+"-"+release;
        String url = "/logs/sources/" + applicationName + "/container/" + container;
        ResultActions resultActions =
                mockMvc.perform(get(url).session(session).contentType(MediaType.APPLICATION_JSON));
        resultActions.andExpect(status().isOk());
        String contentAsString = resultActions.andReturn().getResponse().getContentAsString();
        logger.info(contentAsString);
        Assert.assertTrue(contentAsString.contains(fileToCheck));
    }

}