/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.function.Function;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetContextBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTargetBuilder;
import org.hibernate.search.engine.search.DocumentReference;

class MappedIndexSearchTargetBuilderImpl<R, O> implements MappedIndexSearchTargetBuilder<R, O> {
	private final IndexSearchTargetContextBuilder delegate;
	private final Function<DocumentReference, R> documentReferenceTransformer;

	MappedIndexSearchTargetBuilderImpl(IndexManagerImplementor<?> firstIndexManager,
			MappingContextImplementor mappingContext,
			Function<DocumentReference, R> documentReferenceTransformer) {
		this.delegate = firstIndexManager.createSearchTargetContextBuilder( mappingContext );
		this.documentReferenceTransformer = documentReferenceTransformer;
	}

	void add(IndexManagerImplementor<?> indexManager) {
		indexManager.addToSearchTarget( delegate );
	}

	@Override
	public MappedIndexSearchTarget<R, O> build() {
		return new MappedIndexSearchTargetImpl<>( delegate.build(), documentReferenceTransformer );
	}
}
