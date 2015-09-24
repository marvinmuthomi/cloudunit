/*
 * LICENCE : CloudUnit is available under the Gnu Public License GPL V3 : https://www.gnu.org/licenses/gpl.txt
 *     but CloudUnit is licensed too under a standard commercial license.
 *     Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 *     If you are not sure whether the GPL is right for you,
 *     you can always test our software under the GPL and inspect the source code before you contact us
 *     about purchasing a commercial license.
 *
 *     LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 *     or promote products derived from this project without prior written permission from Treeptik.
 *     Products or services derived from this software may not be called "CloudUnit"
 *     nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 *     For any questions, contact us : contact@treeptik.fr
 */

package fr.treeptik.cloudunit.initializer;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.*;
import java.util.EnumSet;

/**
 * @author Nicolas MULLER
 */
public class ApplicationConfig implements WebApplicationInitializer {
    private static final String DISPATCHER_SERVLET_NAME = "dispatcher";
    private static final String DISPATCHER_SERVLET_MAPPING = "/";

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(ApplicationContext.class);

        ServletRegistration.Dynamic dispatcher = servletContext.addServlet(DISPATCHER_SERVLET_NAME, new DispatcherServlet(rootContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping(DISPATCHER_SERVLET_MAPPING);
        dispatcher.setAsyncSupported(true);

        FilterRegistration.Dynamic security = servletContext.addFilter("springSecurityFilterChain", new DelegatingFilterProxy());
        EnumSet<DispatcherType> securityDispatcherTypes = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);

        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/user/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/file/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/logs/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/messages/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/application/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/server/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/snapshot/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/module/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/admin/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/image/*");
        security.addMappingForUrlPatterns(securityDispatcherTypes, false, "/nopublic/*");

        security.setAsyncSupported(true);

        servletContext.addListener(new ContextLoaderListener(rootContext));
    }
}