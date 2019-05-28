/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.scope.spi.IndexSearchScopeBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexSearchScopeBuilder;

class MappedIndexSearchScopeBuilderImpl<R, E> implements MappedIndexSearchScopeBuilder<R, E> {
	private final IndexSearchScopeBuilder delegate;

	MappedIndexSearchScopeBuilderImpl(IndexManagerImplementor<?> firstIndexManager,
			MappingContextImplementor mappingContext) {
		this.delegate = firstIndexManager.createSearchScopeBuilder( mappingContext );
	}

	void add(IndexManagerImplementor<?> indexManager) {
		indexManager.addTo( delegate );
	}

	@Override
	public MappedIndexSearchScope<R, E> build() {
		return new MappedIndexSearchScopeImpl<>( delegate.build() );
	}
}
