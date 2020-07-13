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
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchEntityProjection<E> extends AbstractElasticsearchProjection<Object, E> {

	private final DocumentReferenceExtractionHelper helper;

	private ElasticsearchEntityProjection(ElasticsearchSearchContext searchContext,
			DocumentReferenceExtractionHelper helper) {
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

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		return projectionHitMapper.planLoading( helper.extract( hit, context ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public E transform(LoadingResult<?> loadingResult, Object extractedData,
			SearchProjectionTransformContext context) {
		E loaded = (E) loadingResult.get( extractedData );
		if ( loaded == null ) {
			context.reportFailedLoad();
		}
		return loaded;
	}

	static class Builder<E> extends AbstractElasticsearchProjection.AbstractBuilder<E>
			implements EntityProjectionBuilder<E> {

		private final ElasticsearchEntityProjection<E> projection;

		Builder(ElasticsearchSearchContext searchContext, DocumentReferenceExtractionHelper helper) {
			super( searchContext );
			this.projection = new ElasticsearchEntityProjection<>( searchContext, helper );
		}

		@Override
		public SearchProjection<E> build() {
			return projection;
		}
	}
}
