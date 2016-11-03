package fr.treeptik.cloudunit.deployments;

import static fr.treeptik.cloudunit.utils.TestUtils.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.Filter;

import org.junit.After;
import org.junit.Before;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import fr.treeptik.cloudunit.dto.ApplicationResource;
import fr.treeptik.cloudunit.dto.DeploymentResource;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.initializer.CloudUnitApplicationContext;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.UserService;
import fr.treeptik.cloudunit.test.ApplicationTemplate;
import fr.treeptik.cloudunit.utils.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        CloudUnitApplicationContext.class,
        MockServletContext.class
        })
@ActiveProfiles("integration")
public abstract class AbstractDeploymentControllerIT {
    private static final String WAR_FILE_URL_TEMPLATE =
            "https://github.com/Treeptik/CloudUnit/releases/download/1.0/%s.war";
    
    protected final Logger logger = LoggerFactory.getLogger(AbstractDeploymentControllerIT.class);

    @Value("${suffix.cloudunit.io}")
    protected String domain;
    protected final String serverType;
    
    @Inject
    private WebApplicationContext context;
    @Inject
    private AuthenticationManager authenticationManager;
    @Inject
    private Filter springSecurityFilterChain;
    @Inject
    private UserService userService;
    
    protected String applicationName;
    
    protected MockMvc mockMvc;
    protected MockHttpSession session;

    protected ApplicationTemplate applicationTemplate;
    protected ApplicationResource application;

    public AbstractDeploymentControllerIT(String serverType) {
        this.serverType = serverType;
    }
    
    @Before
    public void setUp() {
        applicationName = "App" + new Random().nextInt(100000);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
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
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Test
    public void test_deploySimpleWithoutContextApplicationTest() throws Exception {
        deploySimpleApplicationTest("ROOT");
    }

    @Test
    public void test_deploySimpleWithContextApplicationTest() throws Exception {
        deploySimpleApplicationTest("helloworld");
    }

    @Test
    public void test_deployMysql55_BasedApplicationTest() throws Exception {
        deployApplicationWithModule("mysql-5-5", "pizzashop-mysql", "Pizzas");
    }

    @Test
    public void test_deployMysql56_BasedApplicationTest() throws Exception {
        deployApplicationWithModule("mysql-5-6", "pizzashop-mysql", "Pizzas");
    }

    @Test
    public void test_deployMysql57_BasedApplicationTest() throws Exception {
        deployApplicationWithModule("mysql-5-7", "pizzashop-mysql", "Pizzas");
    }

    @Test
    public void test_deployPostGres93BasedApplicationTest() throws Exception {
        deployApplicationWithModule("postgresql-9-3", "pizzashop-postgres", "Pizzas");
    }

    @Test
    public void test_deployPostGres94BasedApplicationTest() throws Exception {
        deployApplicationWithModule("postgresql-9-4", "pizzashop-postgres", "Pizzas");
    }

    @Test
    public void test_deployPostGres95BasedApplicationTest() throws Exception {
        deployApplicationWithModule("postgresql-9-5", "pizzashop-postgres", "Pizzas");
    }

    private void deployApplicationWithModule(String module, String appName, String keywordInPage) throws Exception {
        createApplication();
        try {
            // add the module before deploying war
            addModule(module)
                    .andExpect(status().isCreated());
    
            // deploy the war
            logger.info("Deploy a(n) {} based application", module);
            ResultActions result = deploy(appName);
            result.andExpect(status().isCreated());
            
            DeploymentResource deployment = applicationTemplate.getDeployment(result);
            
            String url = deployment.getLink("open").getHref();
            
            String expectedUrl = String.format("http://%s-johndoe-admin%s/%s",
                    applicationName.toLowerCase(),
                    domain,
                    contextPath(appName));
            
            assertEquals(expectedUrl, url);
    
            Optional<String> contentPage = TestUtils.waitForContent(url);
            assertTrue(contentPage.isPresent());
            assertThat(contentPage.get(), containsString(keywordInPage));    
        } finally {
            deleteApplication();
        }
    }

    private void deploySimpleApplicationTest(String appName) throws Exception {
        createApplication();
        try {
            logger.info("Deploy an helloworld application");
            ResultActions result = deploy(appName);
            result.andExpect(status().isCreated());
            
            DeploymentResource deployment = applicationTemplate.getDeployment(result);
            
            String url = deployment.getLink("open").getHref();
            
            String expectedUrl = String.format("http://%s-johndoe-admin%s/%s",
                    applicationName.toLowerCase(),
                    domain,
                    contextPath(appName));
            
            assertEquals(expectedUrl, url);
            
            String content = getUrlContentPage(url);
            
            assertThat(content, containsString("CloudUnit PaaS"));
        } finally {
            deleteApplication();
        }
    }

    protected void createApplication() throws Exception {
        logger.info("Create Tomcat server");
        application = applicationTemplate.createAndAssumeApplication(applicationName, serverType);
    }

    protected void deleteApplication() throws Exception {
        logger.info("Delete application : " + applicationName);
        applicationTemplate.removeApplication(application)
            .andExpect(status().isNoContent());
    }

    protected ResultActions deploy(String appName) throws Exception {
        logger.info("Deploy application : " + appName);
        return applicationTemplate.addDeployment(
                application,
                "/" + contextPath(appName),
                String.format(WAR_FILE_URL_TEMPLATE, appName));
    }

    private String contextPath(String appName) {
        return appName.equals("ROOT") ? "" : appName;
    }

    protected ResultActions addModule(String moduleName) throws Exception {
        logger.info("Add module : " + moduleName);
        return applicationTemplate.addModule(application, moduleName);
    }
}