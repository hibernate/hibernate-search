/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.Optional;

import org.hibernate.search.engine.mapper.mapping.building.impl.MappedIndexManagerBuilderImpl;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerFactory;

class MappedIndexManagerFactoryImpl implements MappedIndexManagerFactory {
	private final IndexManagerBuildingStateHolder indexManagerBuildingStateHolder;

	MappedIndexManagerFactoryImpl(IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
		this.indexManagerBuildingStateHolder = indexManagerBuildingStateHolder;
	}

	@Override
	public MappedIndexManagerBuilder<?> createMappedIndexManager(IndexedEntityBindingMapperContext mapperContext,
			Optional<String> backendName, String indexName, boolean multiTenancyEnabled) {
		return new MappedIndexManagerBuilderImpl<>(
				mapperContext,
				indexManagerBuildingStateHolder.getIndexManagerBuildingState(
						backendName, indexName, multiTenancyEnabled
				)
		);
	}
}
