/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;

/**
 * A builder for search queries.
 *
 * @param <H> The type of query results
 */
public interface SearchQueryBuilder<H> {

	void predicate(SearchPredicate predicate);

	void sort(SearchSort sort);

	<A> void aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation);

	void addRoutingKey(String routingKey);

	void truncateAfter(long timeout, TimeUnit timeUnit);

	void failAfter(long timeout, TimeUnit timeUnit);

	void totalHitCountThreshold(long totalHitCountThreshold);

	SearchQuery<H> build();

}
