/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchEntityProjection<E> extends AbstractElasticsearchProjection<E>
		implements ElasticsearchSearchProjection.Extractor<Object, E> {

	private final DocumentReferenceExtractionHelper helper;

	private ElasticsearchEntityProjection(ElasticsearchSearchIndexScope<?> scope,
			DocumentReferenceExtractionHelper helper) {
		super( scope );
		this.helper = helper;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, E> request(JsonObject requestBody, ProjectionRequestContext context) {
		helper.request( requestBody, context );
		return this;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			JsonObject source, ProjectionExtractContext context) {
		return projectionHitMapper.planLoading( helper.extract( hit, context ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public E transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			ProjectionTransformContext context) {
		E loaded = (E) loadingResult.get( extractedData );
		if ( loaded == null ) {
			context.reportFailedLoad();
		}
		return loaded;
	}

	static class Builder<E> extends AbstractElasticsearchProjection.AbstractBuilder<E>
			implements EntityProjectionBuilder<E> {

		private final ElasticsearchEntityProjection<E> projection;

		Builder(ElasticsearchSearchIndexScope<?> scope, DocumentReferenceExtractionHelper helper) {
			super( scope );
			this.projection = new ElasticsearchEntityProjection<>( scope, helper );
		}

		@Override
		public SearchProjection<E> build() {
			return projection;
		}
	}
}
