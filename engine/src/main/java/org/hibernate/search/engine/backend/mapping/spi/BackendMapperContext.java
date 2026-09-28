package org.hibernate.search.engine.backend.mapping.spi;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;

public interface BackendMapperContext {
	BackendMappingHints hints();
}
