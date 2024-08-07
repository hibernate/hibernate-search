/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.SearchException;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final ProjectionExtractionHelper<String> mappedTypeNameExtractionHelper;
	private final DocumentReferenceExtractionHelper documentReferenceExtractionHelper;
	private final ProjectionExtractionHelper<String> idProjectionExtractionHelper;

	public ElasticsearchSearchProjectionBuilderFactory(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchIndexScope<?> scope) {
		this.scope = scope;
		this.mappedTypeNameExtractionHelper = searchProjectionBackendContext.createMappedTypeNameExtractionHelper( scope );
		this.documentReferenceExtractionHelper =
				searchProjectionBackendContext.createDocumentReferenceExtractionHelper( mappedTypeNameExtractionHelper );
		this.idProjectionExtractionHelper = searchProjectionBackendContext.idProjectionExtractionHelper();
	}

	@Override
	public SearchProjection<DocumentReference> documentReference() {
		return new ElasticsearchDocumentReferenceProjection( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <E> SearchProjection<E> entityLoading() {
		return new ElasticsearchEntityLoadingProjection<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <R> SearchProjection<R> entityReference() {
		return new ElasticsearchEntityReferenceProjection<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <I> SearchProjection<I> id(Class<I> requestedIdentifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new ElasticsearchIdProjection<>( scope, idProjectionExtractionHelper,
				identifier.mappingProjectionConverter().withConvertedType( requestedIdentifierType, identifier ) );
	}

	@Override
	public SearchProjection<Float> score() {
		return new ElasticsearchScoreProjection( scope );
	}

	@Override
	public CompositeProjectionBuilder composite() {
		return new ElasticsearchCompositeProjection.Builder( scope );
	}

	@Override
	public <T> SearchProjection<T> constant(T value) {
		return new ElasticsearchConstantProjection<>( scope, value );
	}

	@Override
	public <T> SearchProjection<T> entityComposite(SearchProjection<T> delegate) {
		return new ElasticsearchEntityCompositeProjection<>(
				scope, ElasticsearchSearchProjection.from( scope, delegate ) );
	}

	@Override
	public <T> SearchProjection<T> throwing(Supplier<SearchException> exceptionSupplier) {
		return new ElasticsearchThrowingProjection<>( scope, exceptionSupplier );
	}

	@Override
	public <T> SearchProjection<T> byTypeName(Map<String, ? extends SearchProjection<? extends T>> inners) {
		Map<String, ElasticsearchSearchProjection<? extends T>> elasticsearchInners = new HashMap<>();
		for ( Map.Entry<String, ? extends SearchProjection<? extends T>> entry : inners.entrySet() ) {
			elasticsearchInners.put( entry.getKey(), ElasticsearchSearchProjection.from( scope, entry.getValue() ) );
		}
		return new ElasticsearchByMappedTypeProjection<>( scope, mappedTypeNameExtractionHelper,
				elasticsearchInners );
	}

	@Override
	public <T> SearchProjection<T> withParameters(
			Function<? super NamedValues, ? extends ProjectionFinalStep<T>> projectionCreator) {
		return new ElasticsearchWithParametersProjection<>( scope, projectionCreator );
	}

	public SearchProjection<JsonObject> source() {
		return new ElasticsearchSourceProjection( scope );
	}

	public SearchProjection<JsonObject> explanation() {
		return new ElasticsearchExplanationProjection( scope );
	}

	public SearchProjection<JsonObject> jsonHit() {
		return new ElasticsearchJsonHitProjection( scope );
	}

}
