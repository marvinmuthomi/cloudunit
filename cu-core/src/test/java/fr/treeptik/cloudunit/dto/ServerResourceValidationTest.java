package fr.treeptik.cloudunit.dto;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Before;
import org.junit.Test;

import fr.treeptik.cloudunit.enums.JavaRelease;

public class ServerResourceValidationTest {
    private Validator validator;

    @Before
    public void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
        
        assumeNotNull(validator);
    }

    @Test
    public void test_okPatchMemory() {
        ServerResource server = new ServerResource();
        server.setJvmMemory(1024L);
        
        Set<ConstraintViolation<ServerResource>> violations = validator.validate(server, ServerResource.Patch.class);
        
        assertThat(violations, empty());
    }
    
    @Test
    public void test_koPatchOptions() {
        ServerResource server = new ServerResource();
        server.setJvmOptions("-Ddonald=duck -XshowSettings");
        
        Set<ConstraintViolation<ServerResource>> violations = validator.validate(server, ServerResource.Patch.class);
        
        assertThat(violations, empty());
    }
    
    @Test
    public void test_koPatchInvalidMemory() {
        ServerResource server = new ServerResource();
        server.setJvmMemory(1000L);
        
        Set<ConstraintViolation<ServerResource>> violations = validator.validate(server, ServerResource.Patch.class);
        
        assertThat(violations, not(empty()));
    }
    
    @Test
    public void test_koPatchInvalidOptions() {
        ServerResource server = new ServerResource();
        server.setJvmOptions("-Ddonald=duck -Xms123m -XshowSettings");
        
        Set<ConstraintViolation<ServerResource>> violations = validator.validate(server, ServerResource.Patch.class);
        
        assertThat(violations, not(empty()));
    }
    
    @Test
    public void test_koPatchInvalidRelease() {
        ServerResource server = new ServerResource();
        server.setJvmRelease("jdk_1.4");
        
        Set<ConstraintViolation<ServerResource>> violations = validator.validate(server, ServerResource.Patch.class);
        
        assertThat(violations, not(empty()));
    }
    
    
    @Test
    public void test_koFullNoMemory() {
        ServerResource server = new ServerResource();
        server.setJvmRelease(JavaRelease.Java8.getVersion());
        
        Set<ConstraintViolation<ServerResource>> violations = validator.validate(server, ServerResource.Full.class);
        
        assertThat(violations, not(empty()));
    }
}
