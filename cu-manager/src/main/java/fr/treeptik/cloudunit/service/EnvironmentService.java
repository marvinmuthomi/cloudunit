package fr.treeptik.cloudunit.service;

import java.util.List;

import fr.treeptik.cloudunit.exception.CheckException;
import fr.treeptik.cloudunit.exception.ServiceException;
import fr.treeptik.cloudunit.model.Application;
import fr.treeptik.cloudunit.model.EnvironmentVariable;
import fr.treeptik.cloudunit.model.User;

public interface EnvironmentService {

    EnvironmentVariable update(Application application, String containerName, Integer id, String value)
            throws ServiceException;

    EnvironmentVariable loadEnvironnment(int id) throws ServiceException, CheckException;

    List<EnvironmentVariable> loadEnvironnmentsByContainer(String containerName) throws ServiceException;

    void delete(Application application, String containerName, int id) throws ServiceException;

    void save(User user, List<EnvironmentVariable> environments, String applicationName, String containerName)
            throws ServiceException;

    EnvironmentVariable save(Application application, String containerName, String variableName, String value)
            throws ServiceException;

    void createInDatabase(List<EnvironmentVariable> environments, String containerName, Application application);

    void delete(User user, List<EnvironmentVariable> envs, String applicationName, String containerName)
            throws ServiceException;

}
