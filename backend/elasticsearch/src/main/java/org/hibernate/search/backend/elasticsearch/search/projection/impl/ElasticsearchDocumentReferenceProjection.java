/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

class ElasticsearchDocumentReferenceProjection
		implements ElasticsearchSearchProjection<DocumentReference, DocumentReference> {

	private final DocumentReferenceExtractorHelper helper;

	ElasticsearchDocumentReferenceProjection(DocumentReferenceExtractorHelper helper) {
		this.helper = helper;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExtractContext context) {
		helper.contributeRequest( requestBody );
	}

	@Override
	public DocumentReference extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		return helper.extractDocumentReference( hit );
	}

	@Override
	public DocumentReference transform(LoadingResult<?> loadingResult, DocumentReference extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
