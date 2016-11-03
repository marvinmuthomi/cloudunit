package fr.treeptik.cloudunit.deployments;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.Test;

import fr.treeptik.cloudunit.utils.TestUtils;

public abstract class AbstractWildflyDeploymentControllerIT extends AbstractDeploymentControllerIT {
    
    public AbstractWildflyDeploymentControllerIT(String serverType) {
        super(serverType);
    }

    @Test
    public void test_DeployEARApplicationTest()
            throws Exception
    {
        logger.info("Deploy a Wicket EAR application");
        
        createApplication();
        try {
            String earUrl = "https://github.com/Treeptik/cloudunit/releases/download/1.0/wildfly-wicket-ear-ear.ear";
            
            applicationTemplate.addDeployment(application, "ROOT", earUrl)            
                .andExpect(status().isCreated());
            
            String deployedAppUrl = String.format("http://%s-johndoe-admin%s/wildfly-wicket-ear-war",
                    applicationName.toLowerCase(),
                    domain);
            Optional<String> content = TestUtils.waitForContent(deployedAppUrl);
            
            assertTrue(content.isPresent());
            assertThat(content.get(), containsString("Wicket"));
        } finally {
            deleteApplication();
        }
    }

}
