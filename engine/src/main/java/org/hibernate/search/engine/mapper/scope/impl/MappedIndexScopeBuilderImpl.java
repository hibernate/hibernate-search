/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.scope.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;

public class MappedIndexScopeBuilderImpl<R, E> implements MappedIndexScopeBuilder<R, E> {
	private final IndexScopeBuilder delegate;

	public MappedIndexScopeBuilderImpl(IndexManagerImplementor firstIndexManager,
			BackendMappingContext mappingContext) {
		this.delegate = firstIndexManager.createScopeBuilder( mappingContext );
	}

	public void add(IndexManagerImplementor indexManager) {
		indexManager.addTo( delegate );
	}

	@Override
	public MappedIndexScope<R, E> build() {
		return new MappedIndexScopeImpl<>( delegate.build() );
	}
}
