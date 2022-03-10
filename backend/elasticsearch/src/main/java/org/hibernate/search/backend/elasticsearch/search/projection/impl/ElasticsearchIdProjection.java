/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchIdProjection<I> extends AbstractElasticsearchProjection<String, I> {

	private final ProjectionExtractionHelper<String> extractionHelper;
	private final ProjectionConverter<String, ? extends I> converter;

	private ElasticsearchIdProjection(ElasticsearchSearchIndexScope<?> scope,
			ProjectionExtractionHelper<String> extractionHelper,
			ProjectionConverter<String, ? extends I> converter) {
		super( scope );
		this.extractionHelper = extractionHelper;
		this.converter = converter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(JsonObject requestBody, ProjectionRequestContext context) {
		extractionHelper.request( requestBody, context );
	}

	@Override
	public String extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			ProjectionExtractContext context) {
		return extractionHelper.extract( hit, context );
	}

	@Override
	public I transform(LoadingResult<?, ?> loadingResult, String extractedData,
			ProjectionTransformContext context) {
		return converter.fromDocumentValue( extractedData, context.fromDocumentValueConvertContext() );
	}

	static class Builder<I> extends AbstractBuilder<I> implements IdProjectionBuilder<I> {

		private final ElasticsearchIdProjection<I> projection;

		Builder(ElasticsearchSearchIndexScope<?> scope, ProjectionExtractionHelper<String> extractionHelper,
				ProjectionConverter<String, I> converter) {
			super( scope );
			this.projection = new ElasticsearchIdProjection<>( scope, extractionHelper, converter );
		}

		@Override
		public SearchProjection<I> build() {
			return projection;
		}
	}
}
