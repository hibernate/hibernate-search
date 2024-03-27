/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.dsl;

import org.hibernate.search.engine.search.predicate.dsl.ExtendedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;

import org.apache.lucene.search.Query;

/**
 * A factory for search predicates with some Lucene-specific methods.
 */
public interface LuceneSearchPredicateFactory extends ExtendedSearchPredicateFactory<LuceneSearchPredicateFactory> {

	/**
	 * Create a predicate from a Lucene {@link Query}.
	 *
	 * @param query A Lucene query.
	 * @return The final step of the predicate DSL.
	 */
	PredicateFinalStep fromLuceneQuery(Query query);

}
