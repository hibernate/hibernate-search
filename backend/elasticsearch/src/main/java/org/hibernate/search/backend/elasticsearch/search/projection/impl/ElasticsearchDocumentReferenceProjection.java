/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

class ElasticsearchDocumentReferenceProjection
		implements ElasticsearchSearchProjection<DocumentReference, DocumentReference> {

	private final Set<String> indexNames;
	private final DocumentReferenceExtractorHelper helper;

	ElasticsearchDocumentReferenceProjection(Set<String> indexNames, DocumentReferenceExtractorHelper helper) {
		this.indexNames = indexNames;
		this.helper = helper;
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		helper.requestDocumentReference( requestBody );
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

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}
}
