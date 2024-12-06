/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.dsl.impl;

import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregationIndexScope;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.AbstractSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;

public class LuceneSearchAggregationFactoryImpl
		extends AbstractSearchAggregationFactory<
				LuceneSearchAggregationFactory,
				LuceneSearchAggregationIndexScope<?>,
				LuceneSearchPredicateFactory>
		implements LuceneSearchAggregationFactory {

	public LuceneSearchAggregationFactoryImpl(
			SearchAggregationDslContext<LuceneSearchAggregationIndexScope<?>, LuceneSearchPredicateFactory> dslContext) {
		super( dslContext );
	}

	@Override
	public LuceneSearchAggregationFactory withRoot(String objectFieldPath) {
		return new LuceneSearchAggregationFactoryImpl( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}

	// Empty: no extension at the moment.

}
