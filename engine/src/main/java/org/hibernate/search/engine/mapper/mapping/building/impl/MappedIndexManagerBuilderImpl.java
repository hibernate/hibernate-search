/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.impl.MappedIndexManagerImpl;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;

public class MappedIndexManagerBuilderImpl implements MappedIndexManagerBuilder {
	private final IndexedEntityBindingContextImpl bindingContext;
	private final IndexManagerBuildingState indexManagerBuildingState;

	public MappedIndexManagerBuilderImpl(IndexedEntityBindingMapperContext mapperContext,
			IndexManagerBuildingState indexManagerBuildingState) {
		this.bindingContext = new IndexedEntityBindingContextImpl(
				mapperContext, indexManagerBuildingState.getSchemaRootNodeBuilder()
		);
		this.indexManagerBuildingState = indexManagerBuildingState;
	}

	@Override
	public String getIndexName() {
		return indexManagerBuildingState.getIndexName();
	}

	@Override
	public IndexedEntityBindingContext getRootBindingContext() {
		return bindingContext;
	}

	@Override
	public MappedIndexManager build() {
		return new MappedIndexManagerImpl(
				indexManagerBuildingState.build()
		);
	}
}
