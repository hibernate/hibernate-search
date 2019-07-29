/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionTransformContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

/**
 * The context holding all the useful information pertaining to the extraction of data from
 * the response to the Elasticsearch search query.
 */
class ElasticsearchSearchQueryExtractContext implements AggregationExtractContext {

	private final ElasticsearchSearchQueryRequestContext requestContext;
	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final FromDocumentFieldValueConvertContext convertContext;

	private final JsonObject responseBody;

	ElasticsearchSearchQueryExtractContext(ElasticsearchSearchQueryRequestContext requestContext,
			SessionContextImplementor sessionContext,
			ProjectionHitMapper<?, ?> projectionHitMapper,
			JsonObject responseBody) {
		this.requestContext = requestContext;
		this.projectionHitMapper = projectionHitMapper;
		this.convertContext = new FromDocumentFieldValueConvertContextImpl( sessionContext );
		this.responseBody = responseBody;
	}

	@Override
	public FromDocumentFieldValueConvertContext getConvertContext() {
		return convertContext;
	}

	JsonObject getResponseBody() {
		return responseBody;
	}

	ProjectionHitMapper<?, ?> getProjectionHitMapper() {
		return projectionHitMapper;
	}

	SearchProjectionExtractContext createProjectionExtractContext() {
		return new SearchProjectionExtractContext( requestContext );
	}

	SearchProjectionTransformContext createProjectionTransformContext() {
		return new SearchProjectionTransformContext( convertContext );
	}

}
