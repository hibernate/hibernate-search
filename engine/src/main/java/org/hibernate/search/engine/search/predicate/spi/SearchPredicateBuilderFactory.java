/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * A factory for search predicate builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search predicates.
 *
 * @param <C> The type of query element collector
 */
public interface SearchPredicateBuilderFactory<C> {

	/**
	 * Contribute a predicate builder to a collector.
	 * <p>
	 * Will only ever be called once per collector.
	 *
	 * @param collector The query element collector.
	 * @param predicate The predicate implementation.
	 */
	void contribute(C collector, SearchPredicate predicate);

	MatchAllPredicateBuilder matchAll();

	MatchIdPredicateBuilder id();

	BooleanPredicateBuilder bool();

	SimpleQueryStringPredicateBuilder simpleQueryString();

}
