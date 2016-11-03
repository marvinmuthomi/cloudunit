/*
 * LICENCE : CloudUnit is available under the GNU Affero General Public License : https://gnu.org/licenses/agpl.html
 * but CloudUnit is licensed too under a standard commercial license.
 * Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 * If you are not sure whether the AGPL is right for you,
 * you can always test our software under the AGPL and inspect the source code before you contact us
 * about purchasing a commercial license.
 *
 * LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 * or promote products derived from this project without prior written permission from Treeptik.
 * Products or services derived from this software may not be called "CloudUnit"
 * nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 * For any questions, contact us : contact@treeptik.fr
 */

package fr.treeptik.cloudunit.aspects;

import fr.treeptik.cloudunit.dto.JsonInput;
import fr.treeptik.cloudunit.exception.CheckException;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.ApplicationService;
import fr.treeptik.cloudunit.service.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Annotation needed to verify that an user has a real access to a application for its lifecyle.
 * So we could stop an intrusion if user1 wants to stop an application of user2 for example.
 */
@Component
@Aspect
public class SecurityAnnotationAspect {
    private final Logger logger = LoggerFactory.getLogger(SecurityAnnotationAspect.class);

    @Inject
    private UserService userService;

    @Inject
    private ApplicationService applicationService;

    @Before("@annotation(fr.treeptik.cloudunit.aspects.CloudUnitSecurable) && args(applicationName)")
    public void verifyRelationBetweenUserAndApplicationName(JoinPoint joinPoint, String applicationName) {

        UserDetails principal = null;
        try {
            principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userService.findByLogin(principal.getUsername());

            Application application = applicationService.findByNameAndUser(user, applicationName);
            if (application == null) {
                throw new AccessDeniedException("This application does not exist on this account : " + applicationName + "," + user);
            }
        } catch (ServiceException | CheckException e) {
            logger.error(principal.toString() + ", " + applicationName, e);
        }

    }
    
    @Before("@annotation(fr.treeptik.cloudunit.aspects.CloudUnitSecurable) && args(input)")
    public void verifyRelationBetweenUserAndApplicationName(JoinPoint joinPoint, JsonInput input) {
        String applicationName = input.getApplicationName();
        verifyRelationBetweenUserAndApplicationName(joinPoint, applicationName);
    }


    @Before("@annotation(fr.treeptik.cloudunit.aspects.CloudUnitSecurable) && args(applicationId,..)")
    public void verifyRelationBetweenUserAndApplicationId(JoinPoint joinPoint, Integer applicationId) {
        UserDetails principal = null;
        try {
            principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userService.findByLogin(principal.getUsername());

            Application application = applicationService.findById(applicationId);
            
            if (application == null) {
                return;
            }
            
            if (!application.getUser().equals(user)) {
                throw new AccessDeniedException("You are not authorized to access this application");
            }
        } catch (ServiceException | CheckException e) {
            logger.error(principal.toString() + ", " + applicationId, e);
        }
    }

}
