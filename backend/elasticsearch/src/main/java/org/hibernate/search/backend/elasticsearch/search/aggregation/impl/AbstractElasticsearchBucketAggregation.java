/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractElasticsearchBucketAggregation<K, V> implements ElasticsearchSearchAggregation<Map<K, V>> {

	private final Set<String> indexNames;

	AbstractElasticsearchBucketAggregation(AbstractBuilder<K, V> builder) {
		this.indexNames = builder.searchContext.getHibernateSearchIndexNames();
	}

	@Override
	public JsonObject request(AggregationRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		doRequest( context, outerObject, innerObject );

		return outerObject;
	}

	@Override
	public Map<K, V> extract(JsonObject aggregationResult, AggregationExtractContext context) {
		JsonElement buckets = aggregationResult.get( "buckets" );

		return doExtract( context, aggregationResult, buckets );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	protected abstract void doRequest(AggregationRequestContext context, JsonObject outerObject, JsonObject innerObject);

	protected abstract Map<K, V> doExtract(AggregationExtractContext context,
			JsonObject outerObject, JsonElement buckets);

	public abstract static class AbstractBuilder<K, V> implements SearchAggregationBuilder<Map<K, V>> {

		protected final ElasticsearchSearchContext searchContext;

		public AbstractBuilder(ElasticsearchSearchContext searchContext) {
			this.searchContext = searchContext;
		}

		@Override
		public abstract ElasticsearchSearchAggregation<Map<K, V>> build();
	}
}
