/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.predicate;

import org.apache.lucene.search.Query;

import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;

/**
 * A factory for search predicates with some Lucene-specific methods.
 */
public interface LuceneSearchPredicateFactory extends SearchPredicateFactory {

	/**
	 * Create a predicate from a Lucene {@link Query}.
	 *
	 * @param query A Lucene query.
	 * @return The final step of the predicate DSL.
	 */
	PredicateFinalStep fromLuceneQuery(Query query);
}
