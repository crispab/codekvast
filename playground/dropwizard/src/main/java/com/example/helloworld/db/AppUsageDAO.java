package com.example.helloworld.db;

import com.example.helloworld.core.AppUsage;
import com.google.common.base.Optional;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;

public class AppUsageDAO extends AbstractDAO<AppUsage> {
    public AppUsageDAO(SessionFactory factory) {
        super(factory);
    }

    // TODO: Start using this?
    public Optional<AppUsage> findById(Long id) {
        return Optional.fromNullable(get(id));
    }

    public AppUsage create(AppUsage appUsage) {
        return persist(appUsage);
    }

    public List<AppUsage> findAll() {
        return list(namedQuery("com.example.helloworld.core.AppUsage.findAll"));
    }
}
