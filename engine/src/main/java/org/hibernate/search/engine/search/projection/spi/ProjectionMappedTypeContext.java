package org.hibernate.search.engine.search.projection.spi;

public interface ProjectionMappedTypeContext {

	String name();

	Class<?> javaClass();

	boolean loadingAvailable();

}
