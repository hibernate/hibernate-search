/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.function.Function;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexSearchScopeBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScopeBuilder;
import org.hibernate.search.engine.search.DocumentReference;

class MappedIndexSearchScopeBuilderImpl<R, O> implements MappedIndexSearchScopeBuilder<R, O> {
	private final IndexSearchScopeBuilder delegate;
	private final Function<DocumentReference, R> documentReferenceTransformer;

	MappedIndexSearchScopeBuilderImpl(IndexManagerImplementor<?> firstIndexManager,
			MappingContextImplementor mappingContext,
			Function<DocumentReference, R> documentReferenceTransformer) {
		this.delegate = firstIndexManager.createSearchScopeBuilder( mappingContext );
		this.documentReferenceTransformer = documentReferenceTransformer;
	}

	void add(IndexManagerImplementor<?> indexManager) {
		indexManager.addTo( delegate );
	}

	@Override
	public MappedIndexSearchScope<R, O> build() {
		return new MappedIndexSearchScopeImpl<>( delegate.build(), documentReferenceTransformer );
	}
}
