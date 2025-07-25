/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.engine.search.aggregation.AggregationKey;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractElasticsearchBucketAggregation<K, V>
		extends AbstractElasticsearchNestableAggregation<Map<K, V>> {

	protected static final JsonAccessor<JsonObject> REQUEST_AGGREGATIONS_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).asObject();

	private static final JsonAccessor<JsonObject> REQUEST_REVERSE_NESTED_ACCESSOR =
			JsonAccessor.root().property( "reverse_nested" ).asObject();

	private static final String ROOT_DOC_COUNT_NAME = "root_doc_count";
	private static final JsonAccessor<JsonObject> REQUEST_AGGREGATIONS_ROOT_DOC_COUNT_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).property( ROOT_DOC_COUNT_NAME ).asObject();

	protected static final String INNER_EXTRACTOR = "innerExtractor";

	AbstractElasticsearchBucketAggregation(AbstractBuilder<K, V> builder) {
		super( builder );
	}

	@Override
	protected final JsonObject doRequest(AggregationRequestBuildingContextContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		doRequest( outerObject, innerObject, context );

		if ( isNested() ) {
			JsonObject rootDocCountSubAggregationOuterObject = new JsonObject();
			JsonObject rootDocCountSubAggregationInnerObject = new JsonObject();

			REQUEST_REVERSE_NESTED_ACCESSOR.set( rootDocCountSubAggregationOuterObject, rootDocCountSubAggregationInnerObject );
			REQUEST_AGGREGATIONS_ROOT_DOC_COUNT_ACCESSOR.set( outerObject, rootDocCountSubAggregationOuterObject );
		}

		return outerObject;
	}

	protected abstract void doRequest(JsonObject outerObject, JsonObject innerObject,
			AggregationRequestBuildingContextContext context);

	protected abstract class AbstractBucketExtractor<A, B> extends AbstractExtractor<Map<A, B>> {

		protected AbstractBucketExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter) {
			super( key, nestedPathHierarchy, filter );
		}

		@Override
		protected final Map<A, B> doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
			JsonElement buckets = aggregationResult.get( "buckets" );

			return doExtract( context, buckets );
		}

		protected abstract Map<A, B> doExtract(AggregationExtractContext context, JsonElement buckets);
	}

	public abstract static class AbstractBuilder<K, V>
			extends AbstractElasticsearchNestableAggregation.AbstractBuilder<Map<K, V>> {

		public AbstractBuilder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<?> field) {
			super( scope, field );
		}

		@Override
		public abstract ElasticsearchSearchAggregation<Map<K, V>> build();
	}
}
