package fr.treeptik.cloudunit.servers;

import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import fr.treeptik.cloudunit.config.events.ServerStartEvent;
import fr.treeptik.cloudunit.dao.ApplicationDAO;
import fr.treeptik.cloudunit.dao.ServerDAO;
import fr.treeptik.cloudunit.enums.JavaRelease;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Image;
import fr.treeptik.cloudunit.model.Server;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.DockerService;
import fr.treeptik.cloudunit.service.EnvironmentService;
import fr.treeptik.cloudunit.service.VolumeService;
import fr.treeptik.cloudunit.service.impl.ServerServiceImpl;
import fr.treeptik.cloudunit.utils.CustomPasswordEncoder;

public class ServerServiceImplTest {
    private Mockery mockery;
    
    private ServerServiceImpl serverService;

    private EnvironmentService environmentService;

    private DockerService dockerService;

    private VolumeService volumeService;
    
    private ServerDAO serverDao;
    
    private ApplicationDAO applicationDao;
    
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Before
    public void setUp() {
        mockery = new Mockery();
        serverService = new ServerServiceImpl();
        
        environmentService = mockery.mock(EnvironmentService.class);
        serverService.setEnvironmentService(environmentService);
        
        dockerService = mockery.mock(DockerService.class);
        serverService.setDockerService(dockerService);
        
        volumeService = mockery.mock(VolumeService.class);
        serverService.setVolumeService(volumeService);
        
        serverDao = mockery.mock(ServerDAO.class);
        serverService.setServerDAO(serverDao);
        
        applicationDao = mockery.mock(ApplicationDAO.class);
        serverService.setApplicationDAO(applicationDao);
        
        applicationEventPublisher = mockery.mock(ApplicationEventPublisher.class);
        serverService.setApplicationEventPublisher(applicationEventPublisher);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate() throws ServiceException {
        User user = new User();
        user.setLogin("johndoe");
        user.setPassword(new CustomPasswordEncoder().encode("abc2015"));

        Application application = new Application();
        application.setUser(user);

        Image image = new Image();
        image.setPath("pathToImage");
        
        Server server = new Server();
        server.setJvmMemory(1024L);
        server.setJvmOptions("-Ddonald=duck");
        server.setJvmRelease(JavaRelease.Java8.getVersion());
        server.setName("boo");
        
        server.setImage(image);
        server.setApplication(application);
        
        mockery.checking(new Expectations() {{
            oneOf(environmentService).loadEnvironnmentsByContainer("boo");
            will(returnValue(new ArrayList<>()));
            
            oneOf(dockerService).getEnv("boo", "JAVA_OPTS");
            will(returnValue("-Dhillary=donkey -Xms512m -Xmx512m -Dcom.example=512"));
            
            oneOf(dockerService).stopContainer("boo");
            
            oneOf(dockerService).removeContainer("boo", false);
            
            oneOf(volumeService).loadAllByContainerName("boo");
            will(returnValue(new ArrayList<>()));
            
            oneOf(dockerService).createServer(
                    with(equal("boo")),
                    with(same(server)),
                    with(equal("pathToImage")),
                    with(same(user)),
                    (List<String>) with(hasItem("JAVA_OPTS=-Ddonald=duck -Xms1024m -Xmx1024m -Dcom.example=512")),
                    with(equal(false)),
                    with(any(List.class)));
            
            oneOf(dockerService).startServer("boo", server);
            will(returnValue(server));
            
            oneOf(dockerService).execCommand(with(equal("boo")), with(any(String.class)));
            
            atLeast(1).of(serverDao).save(server);
            will(returnValue(server));
            
            atLeast(1).of(applicationDao).saveAndFlush(application);
            will(returnValue(application));
            
            oneOf(applicationEventPublisher).publishEvent(with(any(ServerStartEvent.class)));
        }});
        
        serverService.update(server);
        
        mockery.assertIsSatisfied();
    }
}
