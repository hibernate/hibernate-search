/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchEntityReferenceProjection<R> extends AbstractElasticsearchProjection<R>
		implements ElasticsearchSearchProjection.Extractor<DocumentReference, R> {

	private final DocumentReferenceExtractionHelper helper;

	private ElasticsearchEntityReferenceProjection(ElasticsearchSearchIndexScope<?> scope, DocumentReferenceExtractionHelper helper) {
		super( scope );
		this.helper = helper;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, R> request(JsonObject requestBody, ProjectionRequestContext context) {
		helper.request( requestBody, context );
		return this;
	}

	@Override
	public DocumentReference extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			ProjectionExtractContext context) {
		return helper.extract( hit, context );
	}

	@Override
	@SuppressWarnings("unchecked")
	public R transform(LoadingResult<?, ?> loadingResult, DocumentReference extractedData,
			ProjectionTransformContext context) {
		return (R) loadingResult.convertReference( extractedData );
	}

	static class Builder<R> extends AbstractElasticsearchProjection.AbstractBuilder<R>
			implements EntityReferenceProjectionBuilder<R> {

		private final ElasticsearchEntityReferenceProjection<R> projection;

		Builder(ElasticsearchSearchIndexScope<?> scope, DocumentReferenceExtractionHelper helper) {
			super( scope );
			this.projection = new ElasticsearchEntityReferenceProjection<>( scope, helper );
		}

		@Override
		public SearchProjection<R> build() {
			return projection;
		}
	}
}
