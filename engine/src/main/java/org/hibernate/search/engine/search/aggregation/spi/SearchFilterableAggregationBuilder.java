package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface SearchFilterableAggregationBuilder<A> extends SearchAggregationBuilder<A> {

	void filter(SearchPredicate filter);

}
