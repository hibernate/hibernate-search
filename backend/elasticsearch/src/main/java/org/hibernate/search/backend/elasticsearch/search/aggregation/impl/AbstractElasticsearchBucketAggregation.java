/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractElasticsearchBucketAggregation<K, V>
		extends AbstractElasticsearchNestableAggregation<Map<K, V>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonObject> REQUEST_REVERSE_NESTED_ACCESSOR =
			JsonAccessor.root().property( "reverse_nested" ).asObject();

	private static final String ROOT_DOC_COUNT_NAME = "root_doc_count";
	private static final JsonAccessor<JsonObject> REQUEST_AGGREGATIONS_ROOT_DOC_COUNT_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).property( ROOT_DOC_COUNT_NAME ).asObject();
	private static final JsonAccessor<Long> RESPONSE_DOC_COUNT_ACCESSOR =
			JsonAccessor.root().property( "doc_count" ).asLong();
	private static final JsonAccessor<Long> RESPONSE_ROOT_DOC_COUNT_ACCESSOR =
			JsonAccessor.root().property( ROOT_DOC_COUNT_NAME ).property( "doc_count" ).asLong();

	AbstractElasticsearchBucketAggregation(AbstractBuilder<K, V> builder) {
		super( builder );
	}

	@Override
	protected final JsonObject doRequest(AggregationRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		doRequest( outerObject, innerObject );

		if ( isNested() ) {
			JsonObject rootDocCountSubAggregationOuterObject = new JsonObject();
			JsonObject rootDocCountSubAggregationInnerObject = new JsonObject();

			REQUEST_REVERSE_NESTED_ACCESSOR.set( rootDocCountSubAggregationOuterObject, rootDocCountSubAggregationInnerObject );
			REQUEST_AGGREGATIONS_ROOT_DOC_COUNT_ACCESSOR.set( outerObject, rootDocCountSubAggregationOuterObject );
		}

		return outerObject;
	}

	@Override
	protected final Map<K, V> doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
		JsonElement buckets = aggregationResult.get( "buckets" );

		return doExtract( context, buckets );
	}

	protected abstract void doRequest(JsonObject outerObject, JsonObject innerObject);

	protected abstract Map<K, V> doExtract(AggregationExtractContext context, JsonElement buckets);

	protected final long getBucketDocCount(JsonObject bucket) {
		if ( isNested() ) {
			// We must return the number of root documents,
			// not the number of leaf documents that Elasticsearch returns by default.
			return RESPONSE_ROOT_DOC_COUNT_ACCESSOR.get( bucket )
					.orElseThrow( log::elasticsearchResponseMissingData );
		}
		else {
			return RESPONSE_DOC_COUNT_ACCESSOR.get( bucket )
					.orElseThrow( log::elasticsearchResponseMissingData );
		}
	}

	public abstract static class AbstractBuilder<K, V>
			extends AbstractElasticsearchNestableAggregation.AbstractBuilder<Map<K, V>> {

		public AbstractBuilder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field) {
			super( scope, field );
		}

		@Override
		public abstract ElasticsearchSearchAggregation<Map<K, V>> build();
	}
}
