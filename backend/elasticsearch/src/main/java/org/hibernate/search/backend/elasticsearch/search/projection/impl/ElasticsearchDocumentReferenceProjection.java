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

import com.google.gson.JsonObject;

class ElasticsearchDocumentReferenceProjection
		extends AbstractElasticsearchProjection<DocumentReference>
		implements ElasticsearchSearchProjection.Extractor<DocumentReference, DocumentReference> {

	private final DocumentReferenceExtractionHelper helper;

	ElasticsearchDocumentReferenceProjection(ElasticsearchSearchIndexScope<?> scope,
			DocumentReferenceExtractionHelper helper) {
		super( scope );
		this.helper = helper;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, DocumentReference> request(JsonObject requestBody, ProjectionRequestContext context) {
		helper.request( requestBody, context );
		return this;
	}

	@Override
	public DocumentReference extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			JsonObject source, ProjectionExtractContext context) {
		return helper.extract( hit, context );
	}

	@Override
	public DocumentReference transform(LoadingResult<?, ?> loadingResult, DocumentReference extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}
}
