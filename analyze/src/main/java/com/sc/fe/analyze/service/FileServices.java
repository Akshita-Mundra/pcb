package com.sc.fe.analyze.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sc.fe.analyze.data.entity.Services;
import com.sc.fe.analyze.data.repo.ServicesRepo;

/**
 *
 * @author Hemant
 */
@Service
@Transactional
public class FileServices {

    @Autowired
    private ServicesRepo serviceRepo;

    /**
     * This method find the services
     *
     * @return all the services which found in the database
     */
//    @Cacheable(value = "Services")
    public List<Services> findAll() {
        return serviceRepo.findAll();
    }

    /**
     *
     * @param services the services to store in a database
     */
    public void save(Services services) {
        serviceRepo.save(services);
//        cacheService.evictAllCacheValues("Services");
    }

    /**
     * Removes List of services from the database.
     *
     * @param services the services to remove from a database
     */
    public void deleteAll(List<Services> services) {
        serviceRepo.deleteAll(services);
//        cacheService.evictAllCacheValues("Services");
    }

    /**
     *
     * @param id the specific service to find from database by id
     * @return the services
     */
    public Services getServicesById(int id) {
        return serviceRepo.findById(id).get();
    }

}
