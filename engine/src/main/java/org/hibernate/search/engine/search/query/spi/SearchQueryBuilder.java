/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.common.annotation.Incubating;

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

	@Incubating
	void highlighter(SearchHighlighter queryHighlighter);

	@Incubating
	void highlighter(String highlighterName, SearchHighlighter highlighter);

	SearchQuery<H> build();

}
