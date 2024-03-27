/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Optional;

import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;

/**
 * A factory for {@link org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager} instances,
 * which will be the interface between the mapping and the index when indexing and searching.
 */
public interface MappedIndexManagerFactory {

	MappedIndexManagerBuilder createMappedIndexManager(
			IndexedEntityBindingMapperContext mapperContext,
			BackendMapperContext backendMapperContext,
			Optional<String> backendName, String indexName,
			String mappedTypeName);

}
