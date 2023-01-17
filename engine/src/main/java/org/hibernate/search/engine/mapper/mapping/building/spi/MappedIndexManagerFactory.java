/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
