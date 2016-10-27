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


public class ApplicationCreationRequestValidationTest {
    private Validator validator;

    @Before
    public void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
        
        assumeNotNull(validator);
    }
    
    @Test
    public void test_okNullDisplayName() {
        ApplicationCreationRequest request = new ApplicationCreationRequest();
        request.setServerType("tomcat-8");
        request.setName("app-09");
        
        Set<ConstraintViolation<ApplicationCreationRequest>> violations = validator.validate(request);
        
        assertThat(violations, empty());
    }
    
    @Test
    public void test_okNullName() {
        ApplicationCreationRequest request = new ApplicationCreationRequest();
        request.setServerType("tomcat-8");
        request.setDisplayName("App 09");
        
        Set<ConstraintViolation<ApplicationCreationRequest>> violations = validator.validate(request);
        
        assertThat(violations, empty());
    }
    
    @Test
    public void test_koBlankDisplayName() {
        ApplicationCreationRequest request = new ApplicationCreationRequest();
        request.setServerType("tomcat-8");
        request.setDisplayName("    ");
        
        Set<ConstraintViolation<ApplicationCreationRequest>> violations = validator.validate(request);
        
        assertThat(violations, not(empty()));
    }
    
    @Test
    public void test_koBlankName() {
        ApplicationCreationRequest request = new ApplicationCreationRequest();
        request.setServerType("tomcat-8");
        request.setName("    ");
        
        Set<ConstraintViolation<ApplicationCreationRequest>> violations = validator.validate(request);
        
        assertThat(violations, not(empty()));
    }
    
    @Test
    public void test_koUpperCaseName() {
        ApplicationCreationRequest request = new ApplicationCreationRequest();
        request.setServerType("tomcat-8");
        request.setName("APP-09");
        
        Set<ConstraintViolation<ApplicationCreationRequest>> violations = validator.validate(request);
        
        assertThat(violations, not(empty()));
    }
}
