package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;

public interface LuceneSearchPredicateIndexScope<S extends LuceneSearchPredicateIndexScope<?>>
		extends SearchPredicateIndexScope<S>, LuceneSearchIndexScope<S> {

	@Override
	LuceneSearchPredicateBuilderFactory predicateBuilders();

}
