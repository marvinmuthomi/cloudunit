package fr.treeptik.cloudunit.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import javax.json.Json;

import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.treeptik.cloudunit.dto.ContainerResource;
import fr.treeptik.cloudunit.dto.ModulePortResource;
import fr.treeptik.cloudunit.dto.ModuleResource;

public class ModuleTemplate {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc;
    private final MockHttpSession session;
    
    public ModuleTemplate(MockMvc mockMvc, MockHttpSession session) {
        super();
        this.mockMvc = mockMvc;
        this.session = session;
    }
    
    public ContainerResource getContainer(ModuleResource module) throws Exception {
        String url = module.getLink("container").getHref();
        ResultActions result = mockMvc.perform(get(url).session(session));
        result.andExpect(status().isOk());
        
        String content = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(content, ContainerResource.class);
    }
    
    public Resources<ModulePortResource> getPorts(ModuleResource module) throws Exception {
        String url = module.getLink("ports").getHref();
        ResultActions result = mockMvc.perform(get(url).session(session));
        result.andExpect(status().isOk());
        
        String content = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(content, new TypeReference<Resources<ModulePortResource>>() {});
    }
    
    public ModulePortResource getPort(ResultActions result) throws Exception {
        String content = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(content, ModulePortResource.class);
    }

    public ResultActions publishPort(ModulePortResource port) throws Exception {
        String url = port.getId().getHref();
        String request = Json.createObjectBuilder()
                .add("publishPort", true)
                .build().toString();
        
        ResultActions result = mockMvc.perform(put(url)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
        return result;
    }
}
