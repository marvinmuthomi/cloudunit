package fr.treeptik.cloudunit.dto;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class EnvironmentVariableResourceTest {

    @Test
    public void test_fromDefinitions() {
        String definitions = "HOSTNAME=58f99ebf2f88\n" +
                "CU_HOOKS=/cloudunit/appconf/hooks\n" +
                "CU_USER_HOME=/cloudunit/home\n" +
                "CU_JAVA=/cloudunit/java\n" +
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n" +
                "CU_SHARED=/cloudunit/shared\n" +
                "PWD=/\n" +
                "SHLVL=1\n" +
                "LS_COLORS=rs=0:di=01;34:\n" +
                "HOME=/root\n" +
                "CU_LOGS=/cloudunit/appconf/logs\n" +
                "CU_SCRIPTS=/cloudunit/scripts\n" +
                "_=/usr/bin/env\n";

        List<EnvironmentVariableResource> resources = EnvironmentVariableResource.fromDefinitions(definitions);
        assertThat(resources, containsInAnyOrder(
                    new EnvironmentVariableResource("HOSTNAME", "58f99ebf2f88"),
                    new EnvironmentVariableResource("CU_HOOKS", "/cloudunit/appconf/hooks"),
                    new EnvironmentVariableResource("CU_USER_HOME", "/cloudunit/home"),
                    new EnvironmentVariableResource("CU_JAVA", "/cloudunit/java"),
                    new EnvironmentVariableResource("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"),
                    new EnvironmentVariableResource("CU_SHARED", "/cloudunit/shared"),
                    new EnvironmentVariableResource("PWD", "/"),
                    new EnvironmentVariableResource("SHLVL", "1"),
                    new EnvironmentVariableResource("LS_COLORS", "rs=0:di=01;34:"),
                    new EnvironmentVariableResource("HOME", "/root"),
                    new EnvironmentVariableResource("CU_LOGS", "/cloudunit/appconf/logs"),
                    new EnvironmentVariableResource("CU_SCRIPTS", "/cloudunit/scripts"),
                    new EnvironmentVariableResource("_", "/usr/bin/env")
                ));
    }
    
    @Test
    public void test_fromDefinitionsEmpty() {
        String definitions = "";
        
        List<EnvironmentVariableResource> resources = EnvironmentVariableResource.fromDefinitions(definitions);
        
        assertThat(resources, empty());
    }
    
    @Test
    public void test_fromDefinitionsBlank() {
        String definitions = "     ";
        
        List<EnvironmentVariableResource> resources = EnvironmentVariableResource.fromDefinitions(definitions);
        
        assertThat(resources, empty());
    }
    
    @Test
    public void test_fromDefinitionsJustNewline() {
        String definitions = "\n";
        
        List<EnvironmentVariableResource> resources = EnvironmentVariableResource.fromDefinitions(definitions);
        
        assertThat(resources, empty());
    }
    
    @Test
    public void test_fromDefinitionsJustTab() {
        String definitions = "\t";
        
        List<EnvironmentVariableResource> resources = EnvironmentVariableResource.fromDefinitions(definitions);
        
        assertThat(resources, empty());
    }
}
