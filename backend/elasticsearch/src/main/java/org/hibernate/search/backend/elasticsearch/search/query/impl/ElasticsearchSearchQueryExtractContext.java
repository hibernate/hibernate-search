/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionTransformContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

/**
 * The context holding all the useful information pertaining to the extraction of data from
 * the response to the Elasticsearch search query.
 */
class ElasticsearchSearchQueryExtractContext implements AggregationExtractContext {

	private final ElasticsearchSearchQueryRequestContext requestContext;
	private final ProjectionHitMapper<?> projectionHitMapper;
	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;

	private final JsonObject responseBody;

	ElasticsearchSearchQueryExtractContext(ElasticsearchSearchQueryRequestContext requestContext,
			BackendSessionContext sessionContext,
			ProjectionHitMapper<?> projectionHitMapper,
			JsonObject responseBody) {
		this.requestContext = requestContext;
		this.projectionHitMapper = projectionHitMapper;
		this.fromDocumentValueConvertContext = new FromDocumentValueConvertContextImpl( sessionContext );
		this.responseBody = responseBody;
	}

	@Override
	public FromDocumentValueConvertContext fromDocumentValueConvertContext() {
		return fromDocumentValueConvertContext;
	}

	JsonObject getResponseBody() {
		return responseBody;
	}

	ProjectionHitMapper<?> getProjectionHitMapper() {
		return projectionHitMapper;
	}

	ProjectionExtractContext createProjectionExtractContext() {
		return new ProjectionExtractContext( requestContext );
	}

	ProjectionTransformContext createProjectionTransformContext() {
		return new ProjectionTransformContext( fromDocumentValueConvertContext );
	}

}
