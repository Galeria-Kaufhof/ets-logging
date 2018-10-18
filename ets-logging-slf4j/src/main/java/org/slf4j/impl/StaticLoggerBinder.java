package org.slf4j.impl;

import de.kaufhof.ets.logging.ext.slf4j.LoggerFactoryAdapter;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * Used for SLF4J 1.7.
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

    // Do not set final, see https://github.com/qos-ch/logback/blob/8753103696e5b52ad679405817ed790fd99b3ee6/logback-classic/src/main/java/org/slf4j/impl/StaticLoggerBinder.java#L43
    public static String REQUESTED_API_VERSION = "1.7.25";

    private static StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    private StaticLoggerBinder() { }

    // Do not remove, needed by SLF4j 1.7
    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return SLF4JServiceProviderInitializer.getInstance().getLoggerFactory();
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return LoggerFactoryAdapter.class.getName();
    }

}