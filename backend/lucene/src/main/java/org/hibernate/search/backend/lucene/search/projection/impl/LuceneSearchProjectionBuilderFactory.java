/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneSearchIndexScopeImpl;
import org.hibernate.search.backend.lucene.search.projection.dsl.DocumentTree;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.SearchException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

public class LuceneSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final LuceneSearchIndexScopeImpl scope;

	public LuceneSearchProjectionBuilderFactory(LuceneSearchIndexScopeImpl scope) {
		this.scope = scope;
	}

	@Override
	public SearchProjection<DocumentReference> documentReference() {
		return new LuceneDocumentReferenceProjection( scope );
	}

	@Override
	public <E> SearchProjection<E> entityLoading() {
		return new LuceneEntityLoadingProjection<>( scope );
	}

	@Override
	public <R> SearchProjection<R> entityReference() {
		return new LuceneEntityReferenceProjection<>( scope );
	}

	@Override
	public <I> SearchProjection<I> id(Class<I> requestedIdentifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new LuceneIdProjection<>( scope,
				identifier.projectionConverter().withConvertedType( requestedIdentifierType, identifier ) );
	}

	@Override
	public SearchProjection<Float> score() {
		return new LuceneScoreProjection( scope );
	}

	@Override
	public CompositeProjectionBuilder composite() {
		return new LuceneCompositeProjection.Builder( scope );
	}

	@Override
	public <T> SearchProjection<T> constant(T value) {
		return new LuceneConstantProjection<>( scope, value );
	}

	@Override
	public <T> SearchProjection<T> entityComposite(SearchProjection<T> delegate) {
		return new LuceneEntityCompositeProjection<>( scope, LuceneSearchProjection.from( scope, delegate ) );
	}

	@Override
	public <T> SearchProjection<T> throwing(Supplier<SearchException> exceptionSupplier) {
		return new LuceneThrowingProjection<>( scope, exceptionSupplier );
	}

	@Override
	public <T> SearchProjection<T> byTypeName(Map<String, ? extends SearchProjection<? extends T>> inners) {
		Map<String, LuceneSearchProjection<? extends T>> luceneInners = new HashMap<>();
		for ( Map.Entry<String, ? extends SearchProjection<? extends T>> entry : inners.entrySet() ) {
			luceneInners.put( entry.getKey(), LuceneSearchProjection.from( scope, entry.getValue() ) );
		}
		return new LuceneByMappedTypeProjection<>( scope, luceneInners );
	}

	public SearchProjection<Document> document() {
		return new LuceneDocumentProjection( scope );
	}

	public SearchProjection<Explanation> explanation() {
		return new LuceneExplanationProjection( scope );
	}

	public SearchProjection<DocumentTree> documentTree() {
		return new LuceneDocumentTreeProjection( scope );
	}
}
