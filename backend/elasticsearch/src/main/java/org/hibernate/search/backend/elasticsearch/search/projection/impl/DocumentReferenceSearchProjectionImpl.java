/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

class DocumentReferenceSearchProjectionImpl implements ElasticsearchSearchProjection<DocumentReference> {

	private final DocumentReferenceExtractorHelper helper;

	DocumentReferenceSearchProjectionImpl(DocumentReferenceExtractorHelper helper) {
		this.helper = helper;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		helper.contributeRequest( requestBody );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		return helper.extractDocumentReference( hit );
	}

	@Override
	public DocumentReference transform(LoadingResult<?> loadingResult, Object extractedData) {
		return (DocumentReference) extractedData;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
