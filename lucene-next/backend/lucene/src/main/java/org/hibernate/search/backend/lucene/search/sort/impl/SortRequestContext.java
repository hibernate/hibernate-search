package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;

public interface SortRequestContext {

	PredicateRequestContext toPredicateRequestContext(String absoluteNestedPath);

}
