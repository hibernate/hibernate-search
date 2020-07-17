/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;

import com.google.gson.JsonObject;

class ElasticsearchDocumentReferenceProjection
		extends AbstractElasticsearchProjection<DocumentReference, DocumentReference> {

	private final DocumentReferenceExtractionHelper helper;

	private ElasticsearchDocumentReferenceProjection(ElasticsearchSearchContext searchContext,
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
	public DocumentReference extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		return helper.extract( hit, context );
	}

	@Override
	public DocumentReference transform(LoadingResult<?> loadingResult, DocumentReference extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	static class Builder extends AbstractElasticsearchProjection.AbstractBuilder<DocumentReference>
			implements DocumentReferenceProjectionBuilder {

		private final ElasticsearchDocumentReferenceProjection projection;

		Builder(ElasticsearchSearchContext searchContext, DocumentReferenceExtractionHelper helper) {
			super( searchContext );
			this.projection = new ElasticsearchDocumentReferenceProjection( searchContext, helper );
		}

		@Override
		public SearchProjection<DocumentReference> build() {
			return projection;
		}
	}
}
