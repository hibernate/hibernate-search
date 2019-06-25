/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class ElasticsearchEntityProjection<E> implements ElasticsearchSearchProjection<Object, E> {

	private final Set<String> indexNames;
	private final DocumentReferenceExtractorHelper helper;

	public ElasticsearchEntityProjection(Set<String> indexNames, DocumentReferenceExtractorHelper helper) {
		this.indexNames = indexNames;
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
	public E transform(LoadingResult<?> loadingResult, Object extractedData,
			SearchProjectionTransformContext context) {
		E loaded = (E) loadingResult.getLoaded( extractedData );
		if ( loaded == null ) {
			context.reportFailedLoad();
		}
		return loaded;
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
