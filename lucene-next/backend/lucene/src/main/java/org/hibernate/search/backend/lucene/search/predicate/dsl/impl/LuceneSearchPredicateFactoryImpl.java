/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.dsl.impl;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.dsl.spi.StaticPredicateFinalStep;

import org.apache.lucene.search.Query;

public class LuceneSearchPredicateFactoryImpl
		extends AbstractSearchPredicateFactory<
				LuceneSearchPredicateFactory,
				LuceneSearchPredicateIndexScope<?>>
		implements LuceneSearchPredicateFactory {

	public LuceneSearchPredicateFactoryImpl(SearchPredicateDslContext<LuceneSearchPredicateIndexScope<?>> dslContext) {
		super( dslContext );
	}

	@Override
	public LuceneSearchPredicateFactory withRoot(String objectFieldPath) {
		return new LuceneSearchPredicateFactoryImpl( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ) ) );
	}

	@Override
	public PredicateFinalStep fromLuceneQuery(Query luceneQuery) {
		return new StaticPredicateFinalStep( dslContext.scope().predicateBuilders().fromLuceneQuery( luceneQuery ) );
	}
}
