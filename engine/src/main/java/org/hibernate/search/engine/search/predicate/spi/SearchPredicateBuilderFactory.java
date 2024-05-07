/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

/**
 * A factory for search predicate builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search predicates.
 */
public interface SearchPredicateBuilderFactory {

	MatchAllPredicateBuilder matchAll();

	MatchNonePredicateBuilder matchNone();

	MatchIdPredicateBuilder id();

	BooleanPredicateBuilder bool();

	SimpleQueryStringPredicateBuilder simpleQueryString();

	QueryStringPredicateBuilder queryString();

	WithParametersPredicateBuilder withParameters();
}
