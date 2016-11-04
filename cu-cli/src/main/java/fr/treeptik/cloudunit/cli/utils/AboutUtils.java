package fr.treeptik.cloudunit.cli.utils;

import java.io.IOException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.treeptik.cloudunit.cli.CloudUnitCliException;
import fr.treeptik.cloudunit.cli.Messages;
import fr.treeptik.cloudunit.cli.exception.ManagerResponseException;
import fr.treeptik.cloudunit.cli.processor.InjectLogger;
import fr.treeptik.cloudunit.cli.rest.RestUtils;
import fr.treeptik.cloudunit.dto.AboutResource;

@Component
public class AboutUtils {
    private static final String NO_INFORMATION = Messages.getString("about.NO_INFORMATION");
    
    public static final String ABOUT_URL_FORMAT = "%s/about";
    
    @InjectLogger
    private Logger log;
    
    @Autowired
    private AuthenticationUtils authentificationUtils;
    
    @Autowired
    private RestUtils restUtils;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${cli.version}")
    private String version;
    
    @Value("${cli.timestamp}")
    private String timestamp;
    
    public String getAbout() {
        if (authentificationUtils.isConnected()) {
            try {
                String url = String.format(ABOUT_URL_FORMAT, authentificationUtils.finalHost);
                String result = restUtils.sendGetCommand(url, authentificationUtils.getMap())
                        .get(RestUtils.BODY);
                AboutResource aboutApi = objectMapper.readValue(result, AboutResource.class);
                return MessageConverter.buildAbout(version, timestamp, aboutApi);
            } catch (ManagerResponseException | IOException e) {
                throw new CloudUnitCliException(NO_INFORMATION, e);
            }
        } else {
            return MessageConverter.buildAbout(version, timestamp);
        }
    }

}
