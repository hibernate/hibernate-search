package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;

public interface ElasticsearchSearchPredicateIndexScope<S extends ElasticsearchSearchPredicateIndexScope<?>>
		extends SearchPredicateIndexScope<S> {

	@Override
	ElasticsearchSearchPredicateBuilderFactory predicateBuilders();

}
