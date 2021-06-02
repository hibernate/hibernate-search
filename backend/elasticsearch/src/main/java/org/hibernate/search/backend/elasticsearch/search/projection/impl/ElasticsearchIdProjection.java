/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchIdProjection<I> extends AbstractElasticsearchProjection<String, I> {

	private final ProjectionExtractionHelper<String> extractionHelper;
	private final DocumentIdentifierValueConverter<? extends I> identifierValueConverter;

	private ElasticsearchIdProjection(ElasticsearchSearchIndexScope scope,
			ProjectionExtractionHelper<String> extractionHelper,
			DocumentIdentifierValueConverter<? extends I> identifierValueConverter) {
		super( scope );
		this.extractionHelper = extractionHelper;
		this.identifierValueConverter = identifierValueConverter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		extractionHelper.request( requestBody, context );
	}

	@Override
	public String extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		return extractionHelper.extract( hit, context );
	}

	@Override
	public I transform(LoadingResult<?, ?> loadingResult, String extractedData,
			SearchProjectionTransformContext context) {
		return identifierValueConverter.convertToSource(
				extractedData, context.fromDocumentIdentifierValueConvertContext() );
	}

	static class Builder<I> extends AbstractBuilder<I> implements IdProjectionBuilder<I> {

		private final ElasticsearchIdProjection<I> projection;

		Builder(ElasticsearchSearchIndexScope scope, ProjectionExtractionHelper<String> extractionHelper,
				Class<I> identifierType) {
			super( scope );

			DocumentIdentifierValueConverter<?> identifierValueConverter = scope.idDslConverter(
					ValueConvert.YES );

			// check expected identifier type:
			identifierValueConverter.checkSourceTypeAssignableTo( identifierType );
			@SuppressWarnings("uncheked") // just checked
			DocumentIdentifierValueConverter<? extends I> casted = (DocumentIdentifierValueConverter<? extends I>) identifierValueConverter;

			this.projection = new ElasticsearchIdProjection<>( scope, extractionHelper, casted );
		}

		@Override
		public SearchProjection<I> build() {
			return projection;
		}
	}
}
