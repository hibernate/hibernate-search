/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;


/**
 * An aggregation collector for Lucene.
 *
 * @see LuceneSearchAggregationBuilderFactory#contribute(Object, AggregationKey, SearchAggregation)
 */
public interface LuceneSearchAggregationCollector {

	<A> void collectAggregation(AggregationKey<A> key, LuceneSearchAggregation<A> aggregation);

}
