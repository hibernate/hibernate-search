package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

public interface PojoMappingCollector {

	ContextualFailureCollector failureCollector();

}
