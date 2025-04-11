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

public class LuceneSearchAggregationFactoryImpl<SR>
		extends AbstractSearchAggregationFactory<
				SR,
				LuceneSearchAggregationFactory<SR>,
				LuceneSearchAggregationIndexScope<?>,
				LuceneSearchPredicateFactory<SR>>
		implements LuceneSearchAggregationFactory<SR> {

	public LuceneSearchAggregationFactoryImpl(
			SearchAggregationDslContext<SR,
					LuceneSearchAggregationIndexScope<?>,
					LuceneSearchPredicateFactory<SR>> dslContext) {
		super( dslContext );
	}

	@Override
	public LuceneSearchAggregationFactory<SR> withRoot(String objectFieldPath) {
		return new LuceneSearchAggregationFactoryImpl<SR>( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}

	// Empty: no extension at the moment.

}
