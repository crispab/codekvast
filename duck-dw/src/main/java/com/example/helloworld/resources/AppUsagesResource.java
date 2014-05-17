package com.example.helloworld.resources;


import com.example.helloworld.core.AppUsage;
import com.example.helloworld.db.AppUsageDAO;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/usages")
@Produces(MediaType.APPLICATION_JSON)
public class AppUsagesResource {

    private final AppUsageDAO appUsageDAO;

    public AppUsagesResource(AppUsageDAO appUsageDAO) {this.appUsageDAO = appUsageDAO; }

    @POST
    @UnitOfWork
    public AppUsage createAppUsage(AppUsage appUsage) {
        return appUsageDAO.create(appUsage);
    }

    @GET
    @UnitOfWork
    public List<AppUsage> listAppUsages() {
        return appUsageDAO.findAll();
    }

}
