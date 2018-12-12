package org.slf4j.spi;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;

/**
 * Copy of https://github.com/qos-ch/slf4j/blob/v_1.8.0_beta2/slf4j-api/src/main/java/org/slf4j/spi/SLF4JServiceProvider.java.
 *
 * Remove this file when upgrading to SLF4J 1.8.
 */
public interface SLF4JServiceProvider {

    
    /**
     * Return the instance of {@link ILoggerFactory} that 
     * {@link org.slf4j.LoggerFactory} class should bind to.
     * 
     * @return the instance of {@link ILoggerFactory} that 
     * {@link org.slf4j.LoggerFactory} class should bind to.
     */
    public ILoggerFactory getLoggerFactory();
    
    /**
     * Return the instance of {@link IMarkerFactory} that 
     * {@link org.slf4j.MarkerFactory} class should bind to.
     * 
     * @return the instance of {@link IMarkerFactory} that 
     * {@link org.slf4j.MarkerFactory} class should bind to.
     */
    public IMarkerFactory getMarkerFactory();
    
    
    public MDCAdapter getMDCAdapter();
    
    public String getRequesteApiVersion();
    
    public void initialize();
}
