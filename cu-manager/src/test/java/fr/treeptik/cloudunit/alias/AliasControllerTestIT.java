/*
 * LICENCE : CloudUnit is available under the Affero Gnu Public License GPL V3 : https://www.gnu.org/licenses/agpl-3.0.html
 * but CloudUnit is licensed too under a standard commercial license.
 * Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 * If you are not sure whether the GPL is right for you,
 * you can always test our software under the GPL and inspect the source code before you contact us
 * about purchasing a commercial license.
 *
 * LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 * or promote products derived from this project without prior written permission from Treeptik.
 * Products or services derived from this software may not be called "CloudUnit"
 * nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 * For any questions, contact us : contact@treeptik.fr
 */
package fr.treeptik.cloudunit.alias;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
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

import fr.treeptik.cloudunit.dto.AliasResource;
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
@ContextConfiguration(classes = {
        CloudUnitApplicationContext.class,
        MockServletContext.class
})
@ActiveProfiles("integration")
public class AliasControllerTestIT {
    private final Logger logger = LoggerFactory.getLogger(AliasControllerTestIT.class);

    private final String serverType = "tomcat-8";

    private final String domainSuffix = ".cloudunit.dev";
    
    private final String aliasName = "myalias" + domainSuffix;
    
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

    private static String applicationName1;
    private static String applicationName2;
    
    private ApplicationResource application1;

    private ApplicationResource application2;

    @BeforeClass
    public static void initEnv() {
        int number = new Random().nextInt(10000);
        applicationName1 = "App" + number;
        applicationName2 = "App" + (number + 1);
    }

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

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

        application1 = applicationTemplate.createAndAssumeApplication(applicationName1, serverType);
        application2 = applicationTemplate.createAndAssumeApplication(applicationName2, serverType);        
    }

    @After
    public void tearDown() throws Exception {
        applicationTemplate.deleteApplication(application2);
        applicationTemplate.deleteApplication(application1);

        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Test(timeout = 120000)
    public void test_okCreateAliasTest() throws Exception {
        ResultActions result = applicationTemplate.addAlias(application1, aliasName);
        result.andExpect(status().isCreated());
        
        Resources<AliasResource> aliases = applicationTemplate.getAliases(application1);

        assertThat(aliases.getContent(), hasItem(hasProperty("name", equalTo(aliasName))));
    }        

    @Test
    public void test_koCreateAliasAlreadyExistsSameApp() throws Exception {
        ResultActions result = applicationTemplate.addAlias(application1, aliasName);
        result.andExpect(status().isCreated());
        
        result = applicationTemplate.addAlias(application1, aliasName);
        result.andExpect(status().isBadRequest());
    }
    
    @Test
    public void test_koCreateAliasAlreadyExistsOtherApp() throws Exception {
        ResultActions result = applicationTemplate.addAlias(application1, aliasName);
        result.andExpect(status().isCreated());
        
        result = applicationTemplate.addAlias(application2, aliasName);
        result.andExpect(status().isBadRequest());
    }
    
    public void test_okRemoveSchema(String schema) throws Exception {
        String aliasWithSchema = schema + aliasName;
        
        ResultActions result = applicationTemplate.addAlias(application1, aliasWithSchema);
        result.andExpect(status().isCreated());
        
        AliasResource alias = applicationTemplate.getAlias(result);
        
        assertEquals(aliasName, alias.getName());
    }

    @Test
    public void test_okRemoveSchemaHttp() throws Exception {
        test_okRemoveSchema("http://");
    }
    
    @Test
    public void test_okRemoveSchemaHttps() throws Exception {
        test_okRemoveSchema("https://");
    }
    
    @Test
    public void test_okRemoveSchemaFtp() throws Exception {
        test_okRemoveSchema("ftp://");
    }

    @Test
    public void test_koAddAliasInvalidPrefix() throws Exception {
        String wrongAlias = "hello://" + aliasName;

        ResultActions result = applicationTemplate.addAlias(application1, wrongAlias);
        result.andExpect(status().isBadRequest());
    }
    
    @Test
    public void test_koAddAliasInvalidSyntaxBackslash() throws Exception {
        String wrongAlias = "error:\\" + aliasName;

        ResultActions result = applicationTemplate.addAlias(application1, wrongAlias);
        result.andExpect(status().isBadRequest());
    }
    
    @Test
    public void test_koAddAliasInvalidSyntaxColon() throws Exception {
        String wrongAlias = ":" + aliasName;

        ResultActions result = applicationTemplate.addAlias(application1, wrongAlias);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void test_okAddAliasThenRemove() throws Exception {
        ResultActions result = applicationTemplate.addAlias(application1, aliasName);
        result.andExpect(status().isCreated());
        
        AliasResource alias = applicationTemplate.getAlias(result);

        result = applicationTemplate.removeAlias(alias);
        result.andExpect(status().isNoContent());

        Resources<AliasResource> aliases = applicationTemplate.getAliases(application1);

        assertThat(aliases.getContent(), empty());
    }

    @Test(timeout = 60000)
    public void test_koRemoveNonexistentAlias() throws Exception {
        AliasResource alias = new AliasResource(aliasName);
        alias.add(new Link(application1.getId().getHref() + "/aliases/dzqmokdzq"));
        
        ResultActions result = applicationTemplate.removeAlias(alias);
        result.andExpect(status().isNotFound());        
    }

    @Test(timeout = 60000)
    public void test_okCreateThenRestartApplication() throws Exception {
        ResultActions result = applicationTemplate.addAlias(application1, aliasName);
        result.andExpect(status().isCreated());
        
        applicationTemplate.stopApplication(application1)
            .andExpect(status().isNoContent());
        
        application1 = applicationTemplate.refreshApplication(application1);
        
        applicationTemplate.startApplication(application1)
            .andExpect(status().isNoContent());
        
        Resources<AliasResource> aliases = applicationTemplate.getAliases(application1);

        assertThat(aliases.getContent(), hasItem(hasProperty("name", equalTo(aliasName))));        
    }

}