/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;

import com.google.gson.JsonObject;

/**
 * A search result extractor for ES5.6 to 6.x.
 * <p>
 * Compared to ES7:
 * <ul>
 *     <li>The total hit count is retrieved from hits.total instead of hits.total.value</li>
 * </ul>
 */
class Elasticsearch56SearchResultExtractor<H> extends Elasticsearch7SearchResultExtractor<H> {

	private static final JsonAccessor<Long> HITS_TOTAL_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).asLong();

	Elasticsearch56SearchResultExtractor(
			ElasticsearchSearchQueryRequestContext requestContext,
			ElasticsearchSearchProjection.Extractor<?, H> rootExtractor,
			Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations) {
		super( requestContext, rootExtractor, aggregations );
	}

	@Override
	protected SearchResultTotal extractTotal(JsonObject responseBody) {
		Optional<Long> hitsTotal = HITS_TOTAL_ACCESSOR.get( responseBody );

		return ( hitsTotal.isPresent() )
				? SimpleSearchResultTotal.exact( hitsTotal.get() )
				: SimpleSearchResultTotal.lowerBound( 0L );
	}

}
