package fr.treeptik.cloudunit.cli.integration.environmentVariables;

import static fr.treeptik.cloudunit.cli.integration.ShellMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.shell.core.CommandResult;

import fr.treeptik.cloudunit.cli.integration.AbstractShellIntegrationTest;

public class AbstractEnvironmentVariablesCommandsIT extends AbstractShellIntegrationTest {
    private static final String KEY = "KEY";
    private static final String VALUE = "value";

    protected AbstractEnvironmentVariablesCommandsIT(String serverType) {
        super(serverType);
    }

    @Test
    public void test_shouldCreateEnvironmentVariable() {
        connect();
        createApplication();
        try {
            CommandResult result = createEnvironmentVariable(KEY, VALUE);
    
            Assert.assertThat(result, isSuccessfulCommand());
            Assert.assertThat(result.getResult().toString(), containsString("added"));
            Assert.assertThat(result.getResult().toString(), containsString(KEY));
            Assert.assertThat(result.getResult().toString(), containsString(applicationName.toLowerCase()));
        } finally {
            removeApplication();
            disconnect();
        }
    }

    @Test
    public void test_shouldNotCreateEnvironmentVariableEmptyKey() {
        connect();
        createApplication();
        try {
            CommandResult result = createEnvironmentVariable("", VALUE);
            assertThat(result, isFailedCommand());
        } finally {
            removeApplication();
            disconnect();
        }
    }

    @Test
    public void test_shouldNotCreateEnvironmentVariableForbiddenKey() {
        connect();
        createApplication();
        try {
            CommandResult result = createEnvironmentVariable("azérty", VALUE);
            
            assertThat(result, isFailedCommand());
        } finally {
            removeApplication();
            disconnect();
        }
    }

    @Test
    public void test_shouldRemoveEnvironmentVariable() {
        connect();
        createApplication();
        try {
            CommandResult result;
            
            result = createEnvironmentVariable(KEY, VALUE);
            assumeThat(result, isSuccessfulCommand());
            
            result = removeEnvironmentVariable(KEY);
            
            assertThat(result, isSuccessfulCommand());
            assertThat(result.getResult().toString(), containsString("removed"));
            assertThat(result.getResult().toString(), containsString(KEY));
            assertThat(result.getResult().toString(), containsString(applicationName.toLowerCase()));
        } finally {
            removeApplication();
            disconnect();
        }
    }

    @Test
    public void test_shouldNotRemoveEnvironmentVariableEmptyKey() {
        connect();
        createApplication();
        try {
            CommandResult result;
            
            result = createEnvironmentVariable(KEY, VALUE);
            assumeThat(result, isSuccessfulCommand());
            
            result = removeEnvironmentVariable("");
            assertThat(result, isFailedCommand());
        } finally {
            removeApplication();
            disconnect();
        }
    }

    @Test
    public void test_shouldNotRemoveEnvironmentVariableUnexistingKey() {
        connect();
        createApplication();
        try {
            CommandResult result = removeEnvironmentVariable("azerty");

            assertThat(result, isFailedCommand());
        } finally {
            removeApplication();
            disconnect();
        }
        
    }

    @Test
    public void test_shouldListEnvironmentVariables() {
        connect();
        createApplication();
        try {
            createEnvironmentVariable(KEY, VALUE);
            CommandResult result = listEnvironmentVariables();
            
            assertThat(result, isSuccessfulCommand());
            assertThat(result.getResult().toString(), containsString("1"));
            assertThat(result.getResult().toString(), containsString("found"));
        } finally {
            removeApplication();
            disconnect();
        }
    }

    @Test
    public void test_shouldUpdateEnvironmentVariable() {
        connect();
        createApplication();
        try {
            createEnvironmentVariable(KEY, VALUE);
            CommandResult result = updateEnvironmentVariable(KEY, "'new value'");
            
            assertThat(result, isSuccessfulCommand());
            
            String expected = "OK";
            assertEquals(expected, result.getResult().toString());
        } finally {
            removeApplication();
        }
    }

    @Test
    public void test_shouldNotUpdateEnvironmentVariableEmptyKey() {
        connect();
        createApplication();
        try {
            createEnvironmentVariable(KEY, VALUE);
            CommandResult result = updateEnvironmentVariable("", VALUE);
            
            assertThat(result, isFailedCommand());
        } finally {
            removeApplication();
        }
    }

    @Test
    public void test_shouldNotUpdateEnvironmentVariableUnexistingKey() {
        connect();
        createApplication();
        try {
            createEnvironmentVariable(KEY, VALUE);
            CommandResult result = updateEnvironmentVariable("DZQMOKDZQ", VALUE);

            assertThat(result, isFailedCommand());
        } finally {
            removeApplication();
        }
    }

    private CommandResult createEnvironmentVariable(String key, String value) {
        return getShell().executeCommand(String.format("create-var-env --key %s --value %s", key, value));
    }
    
    private CommandResult removeEnvironmentVariable(String key) {
        return getShell().executeCommand(String.format("rm-var-env --key %s", key));
    }
    
    private CommandResult updateEnvironmentVariable(String key, String value) {
        return getShell().executeCommand(String.format("update-var-env --key %s --value %s",
                key,
                value));
    }
    
    private CommandResult listEnvironmentVariables() {
        return getShell().executeCommand("list-var-env");
    }
}
