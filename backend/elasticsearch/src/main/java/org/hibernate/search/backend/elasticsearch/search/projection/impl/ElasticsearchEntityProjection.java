/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class ElasticsearchEntityProjection<O> implements ElasticsearchSearchProjection<Object, O> {

	private final DocumentReferenceExtractorHelper helper;

	public ElasticsearchEntityProjection(DocumentReferenceExtractorHelper helper) {
		this.helper = helper;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExtractContext context) {
		helper.contributeRequest( requestBody );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		return projectionHitMapper.planLoading( helper.extractDocumentReference( hit ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public O transform(LoadingResult<?> loadingResult, Object extractedData,
			SearchProjectionTransformContext context) {
		return (O) loadingResult.getLoaded( extractedData );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
