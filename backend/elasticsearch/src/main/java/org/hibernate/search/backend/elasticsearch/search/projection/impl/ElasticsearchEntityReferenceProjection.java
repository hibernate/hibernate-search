/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchEntityReferenceProjection<R> extends AbstractElasticsearchProjection<R, R> {

	private final DocumentReferenceExtractionHelper helper;

	private ElasticsearchEntityReferenceProjection(ElasticsearchSearchContext searchContext, DocumentReferenceExtractionHelper helper) {
		super( searchContext );
		this.helper = helper;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		helper.request( requestBody, context );
	}

	@SuppressWarnings("unchecked")
	@Override
	public R extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		return (R) projectionHitMapper.convertReference( helper.extract( hit, context ) );
	}

	@Override
	public R transform(LoadingResult<?> loadingResult, R extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	static class Builder<R> extends AbstractElasticsearchProjection.AbstractBuilder<R>
			implements EntityReferenceProjectionBuilder<R> {

		private final ElasticsearchEntityReferenceProjection<R> projection;

		Builder(ElasticsearchSearchContext searchContext, DocumentReferenceExtractionHelper helper) {
			super( searchContext );
			this.projection = new ElasticsearchEntityReferenceProjection<>( searchContext, helper );
		}

		@Override
		public SearchProjection<R> build() {
			return projection;
		}
	}
}
