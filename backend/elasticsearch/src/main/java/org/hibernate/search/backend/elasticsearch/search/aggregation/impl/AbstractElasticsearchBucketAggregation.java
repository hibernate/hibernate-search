/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractElasticsearchBucketAggregation<K, V>
		extends AbstractElasticsearchNestableAggregation<Map<K, V>> {

	AbstractElasticsearchBucketAggregation(AbstractBuilder<K, V> builder) {
		super( builder );
	}

	@Override
	protected final JsonObject doRequest(AggregationRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		doRequest( context, outerObject, innerObject );

		return outerObject;
	}

	@Override
	protected final Map<K, V> doExtract(JsonObject aggregationResult, AggregationExtractContext context) {
		JsonElement buckets = aggregationResult.get( "buckets" );

		return doExtract( context, aggregationResult, buckets );
	}

	protected abstract void doRequest(AggregationRequestContext context, JsonObject outerObject, JsonObject innerObject);

	protected abstract Map<K, V> doExtract(AggregationExtractContext context,
			JsonObject outerObject, JsonElement buckets);

	public abstract static class AbstractBuilder<K, V>
			extends AbstractElasticsearchNestableAggregation.AbstractBuilder<Map<K, V>> {

		public AbstractBuilder(ElasticsearchSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public abstract ElasticsearchSearchAggregation<Map<K, V>> build();
	}
}
