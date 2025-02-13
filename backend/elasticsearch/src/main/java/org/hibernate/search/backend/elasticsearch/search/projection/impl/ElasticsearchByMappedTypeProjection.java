/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

final class ElasticsearchByMappedTypeProjection<P>
		extends AbstractElasticsearchProjection<P> {

	private final ProjectionExtractionHelper<String> mappedTypeNameHelper;
	private final Map<String, ElasticsearchSearchProjection<? extends P>> inners;

	public ElasticsearchByMappedTypeProjection(ElasticsearchSearchIndexScope<?> scope,
			ProjectionExtractionHelper<String> mappedTypeNameHelper,
			Map<String, ElasticsearchSearchProjection<? extends P>> inners) {
		super( scope );
		this.mappedTypeNameHelper = mappedTypeNameHelper;
		this.inners = inners;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + inners
				+ "]";
	}

	@Override
	public Extractor<?, P> request(JsonObject requestBody, ProjectionRequestContext context) {
		Map<String, Extractor<?, ? extends P>> innerExtractors = new HashMap<>();
		for ( Map.Entry<String, ElasticsearchSearchProjection<? extends P>> entry : inners.entrySet() ) {
			innerExtractors.put( entry.getKey(), entry.getValue().request( requestBody, context ) );
		}
		return new ByMappedTypeExtractor( innerExtractors );
	}

	private final class ByMappedTypeExtractor implements Extractor<DelegateAndExtractedValue<?, P>, P> {
		private final Map<String, Extractor<?, ? extends P>> inners;

		private ByMappedTypeExtractor(Map<String, Extractor<?, ? extends P>> inners) {
			this.inners = inners;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "inners=" + inners
					+ "]";
		}

		@Override
		public DelegateAndExtractedValue<?, P> extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit,
				JsonObject source, ProjectionExtractContext context) {
			String typeName = mappedTypeNameHelper.extract( hit, context );
			Extractor<?, ? extends P> inner = inners.get( typeName );
			if ( inner == null ) {
				throw QueryLog.INSTANCE.unexpectedMappedTypeNameForByMappedTypeProjection( typeName, inners.keySet() );
			}
			return new DelegateAndExtractedValue<>( inner, projectionHitMapper, hit, source, context );
		}

		@Override
		public P transform(LoadingResult<?> loadingResult, DelegateAndExtractedValue<?, P> extracted,
				ProjectionTransformContext context) {
			return extracted.transform( loadingResult, context );
		}
	}

	private static final class DelegateAndExtractedValue<E, P> {
		private final Extractor<E, ? extends P> delegate;
		private final E extractedValue;

		private DelegateAndExtractedValue(Extractor<E, ? extends P> delegate,
				ProjectionHitMapper<?> projectionHitMapper,
				JsonObject hit, JsonObject source, ProjectionExtractContext context) {
			this.delegate = delegate;
			this.extractedValue = delegate.extract( projectionHitMapper, hit, source, context );
		}

		P transform(LoadingResult<?> loadingResult, ProjectionTransformContext context) {
			return delegate.transform( loadingResult, extractedValue, context );
		}
	}
}
