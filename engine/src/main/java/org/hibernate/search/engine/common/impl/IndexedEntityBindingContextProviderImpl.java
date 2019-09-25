/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.mapper.mapping.building.impl.IndexedEntityBindingContextImpl;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContextProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;

class IndexedEntityBindingContextProviderImpl implements IndexedEntityBindingContextProvider {
	private final IndexManagerBuildingStateHolder indexManagerBuildingStateHolder;

	IndexedEntityBindingContextProviderImpl(IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
		this.indexManagerBuildingStateHolder = indexManagerBuildingStateHolder;
	}

	@Override
	public IndexManagerBuildingState<?> getIndexManagerBuildingState(Optional<String> backendName, String indexName,
			boolean multiTenancyEnabled) {
		return indexManagerBuildingStateHolder.getIndexManagerBuildingState(
				backendName, indexName, multiTenancyEnabled
		);
	}

	@Override
	public IndexedEntityBindingContext createIndexedEntityBindingContext(
			IndexedEntityBindingMapperContext mapperContext,
			IndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder) {
		return new IndexedEntityBindingContextImpl( mapperContext, indexSchemaRootNodeBuilder );
	}
}
