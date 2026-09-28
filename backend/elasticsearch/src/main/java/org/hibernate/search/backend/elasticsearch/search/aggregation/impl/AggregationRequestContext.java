package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;

public interface AggregationRequestContext {

	PredicateRequestContext getRootPredicateContext();

	boolean isRootContext();

}
