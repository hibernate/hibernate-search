/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.dsl;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;

public interface LuceneSearchAggregationFactory<SR>
		extends ExtendedSearchAggregationFactory<SR, LuceneSearchAggregationFactory<SR>, LuceneSearchPredicateFactory<SR>> {

}
