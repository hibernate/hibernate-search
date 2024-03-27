/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	public IndexedEntityBindingContext rootBindingContext() {
		return bindingContext;
	}

	@Override
	public MappedIndexManager build() {
		return new MappedIndexManagerImpl(
				indexManagerBuildingState.build()
		);
	}
}
