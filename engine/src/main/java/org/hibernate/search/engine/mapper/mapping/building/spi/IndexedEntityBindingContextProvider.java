/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;

public interface IndexedEntityBindingContextProvider {

	IndexManagerBuildingState<?> getIndexManagerBuildingState(Optional<String> backendName, String indexName,
			boolean multiTenancyEnabled);

	IndexedEntityBindingContext createIndexedEntityBindingContext(
			IndexedEntityBindingMapperContext mapperContext,
			IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder);

}
