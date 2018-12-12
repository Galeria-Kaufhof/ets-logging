package org.slf4j.impl;

import org.slf4j.spi.SLF4JServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringJoiner;

/**
 * Used for SLF4J 1.7.
 */
public class SLF4JServiceProviderInitializer {
    private static SLF4JServiceProvider INSTANCE = initSlf4JServiceProvider();

    public static SLF4JServiceProvider getInstance() {
        return INSTANCE;
    }

    private static SLF4JServiceProvider initSlf4JServiceProvider() {
        List<SLF4JServiceProvider> serviceProviderList = new ArrayList<>();

        for (SLF4JServiceProvider serviceProvider : ServiceLoader.load(SLF4JServiceProvider.class)) {
            serviceProviderList.add(serviceProvider);
        }

        if (serviceProviderList.size() < 1) {
            throw new IllegalArgumentException("Unable to find implementation for " + SLF4JServiceProvider.class.getName() + ". Please see the ETS Logging README for how to set up SLF4J.");
        } else if (serviceProviderList.size() > 1) {
            StringJoiner sj = new StringJoiner(", ");

            for (SLF4JServiceProvider serviceProvider : serviceProviderList) {
                sj.add(serviceProvider.getClass().getName());
            }

            throw new IllegalArgumentException("Found more than one implementation for " + SLF4JServiceProvider.class.getName() + ": " + sj.toString() + ". Please see the ETS Logging README for how to set up SLF4J.");
        } else {
            SLF4JServiceProvider slf4JServiceProvider = serviceProviderList.get(0);
            slf4JServiceProvider.initialize();

            return slf4JServiceProvider;
        }


    }
}
