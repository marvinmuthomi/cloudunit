package fr.treeptik.cloudunit.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.hateoas.Resources;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.EnvironmentVariableResource;

public class ContainerTemplate {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc;
    private final MockHttpSession session;
    
    public ContainerTemplate(MockMvc mockMvc, MockHttpSession session) {
        super();
        this.mockMvc = mockMvc;
        this.session = session;
    }
    
    public Resources<EnvironmentVariableResource> getFullEnvironment(ContainerResource container) throws Exception {
        String url = container.getLink("env").getHref();
        ResultActions result = mockMvc.perform(get(url).session(session));
        result.andExpect(status().isOk());
        String content = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(content, new TypeReference<Resources<EnvironmentVariableResource>>() {});
    }
}
