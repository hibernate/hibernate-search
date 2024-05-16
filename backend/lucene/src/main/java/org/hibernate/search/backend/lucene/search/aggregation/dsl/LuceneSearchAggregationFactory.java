/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.dsl;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;

public interface LuceneSearchAggregationFactory<E>
		extends ExtendedSearchAggregationFactory<E, LuceneSearchAggregationFactory<E>, LuceneSearchPredicateFactory<E>> {

}
