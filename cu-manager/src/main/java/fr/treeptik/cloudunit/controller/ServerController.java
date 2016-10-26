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

package fr.treeptik.cloudunit.controller;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spotify.docker.client.exceptions.DockerException;

import fr.treeptik.cloudunit.aspects.CloudUnitSecurable;
import fr.treeptik.cloudunit.config.events.ApplicationStartEvent;
import fr.treeptik.cloudunit.config.events.ServerStartEvent;
import fr.treeptik.cloudunit.dto.HttpOk;
import fr.treeptik.cloudunit.dto.JsonResponse;
import fr.treeptik.cloudunit.dto.JvmConfigurationResource;
import fr.treeptik.cloudunit.dto.VolumeAssociationDTO;
import fr.treeptik.cloudunit.dto.VolumeResource;
import fr.treeptik.cloudunit.exception.CheckException;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.Server;
import fr.treeptik.cloudunit.model.Status;
import fr.treeptik.cloudunit.model.User;
import fr.treeptik.cloudunit.service.ApplicationService;
import fr.treeptik.cloudunit.service.ServerService;
import fr.treeptik.cloudunit.service.VolumeService;
import fr.treeptik.cloudunit.utils.AuthentificationUtils;

@Controller
@RequestMapping("/applications/{applicationId}/server")
public class ServerController {
	private static final Locale LOCALE = Locale.ENGLISH;

	private Logger logger = LoggerFactory.getLogger(ServerController.class);

	@Inject
	private ApplicationService applicationService;

	@Inject
	private ServerService serverService;

	@Inject
	private VolumeService volumeService;

	@Inject
	private AuthentificationUtils authentificationUtils;

	@Inject
	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Set the JVM Options and Memory
	 *
	 * @param input
	 * @return
	 * @throws ServiceException
	 * @throws CheckException
	 */
	@CloudUnitSecurable
	@RequestMapping(value = "/jvm-configuration", method = RequestMethod.PUT)
	public ResponseEntity<?> setJvmConfiguration(
	        @PathVariable Integer applicationId,
	        @Valid @RequestBody JvmConfigurationResource request)
	        throws ServiceException, CheckException {
		logger.debug("Requested JVM configuration: {}", request);

		User user = authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findById(applicationId);

		authentificationUtils.canStartNewAction(user, application, LOCALE);

		applicationService.setStatus(application, Status.PENDING);

		try {
			Server server = application.getServer();
			serverService.update(server, request.getMemory().getSize(), request.getOptions(), request.getRelease().getVersion(), false);

		} catch (Exception e) {
			applicationService.setStatus(application, Status.FAIL);
		}

		applicationService.setStatus(application, Status.START);

		return ResponseEntity.noContent().build();
	}

	@RequestMapping(value = "/volume/containerName/{containerName}", method = RequestMethod.GET)
	public ResponseEntity<?> getVolume(@PathVariable("containerName") String containerName)
			throws ServiceException, CheckException {
		List<VolumeResource> resource = volumeService.loadAllByContainerName(containerName).stream()
						.map(VolumeResource::new)
						.collect(Collectors.toList());
		return ResponseEntity.ok(resource);
	}

	@CloudUnitSecurable
	@RequestMapping(value = "/volume", method = RequestMethod.PUT)
	@ResponseBody
	@Transactional
	public JsonResponse linkVolumeAssociation(@RequestBody VolumeAssociationDTO volumeAssociationDTO)
			throws ServiceException, CheckException {

		if (logger.isDebugEnabled()) {
			logger.debug("" + volumeAssociationDTO);
		}

		User user = authentificationUtils.getAuthentificatedUser();
		Application application = applicationService.findByNameAndUser(user, volumeAssociationDTO.getApplicationName());

		serverService.addVolume(application, volumeAssociationDTO);

		applicationEventPublisher.publishEvent(new ServerStartEvent(application.getServer()));
		applicationEventPublisher.publishEvent(new ApplicationStartEvent(application));

		return new HttpOk();
	}

	@RequestMapping(value = "/volume/{volumeName}/container/{containerName}", method = RequestMethod.DELETE)
	@ResponseBody
	public JsonResponse removeVolume(@PathVariable("containerName") String containerName,
			@PathVariable("volumeName") String volumeName) throws ServiceException, CheckException {
		if (logger.isDebugEnabled()) {
			logger.debug("" + containerName + " " + volumeName);
		}
		serverService.removeVolume(containerName, volumeName);
		return new HttpOk();
	}

}
