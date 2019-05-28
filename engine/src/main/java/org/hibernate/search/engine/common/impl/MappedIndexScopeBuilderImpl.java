/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;

class MappedIndexScopeBuilderImpl<R, E> implements MappedIndexScopeBuilder<R, E> {
	private final IndexScopeBuilder delegate;

	MappedIndexScopeBuilderImpl(IndexManagerImplementor<?> firstIndexManager,
			MappingContextImplementor mappingContext) {
		this.delegate = firstIndexManager.createScopeBuilder( mappingContext );
	}

	void add(IndexManagerImplementor<?> indexManager) {
		indexManager.addTo( delegate );
	}

	@Override
	public MappedIndexScope<R, E> build() {
		return new MappedIndexScopeImpl<>( delegate.build() );
	}
}
