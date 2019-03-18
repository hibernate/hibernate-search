/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when a simple query string predicate is fully defined.
 * <p>
 * Allows to set options or to {@link #toPredicate() retrieve the predicate}.
 */
public interface SimpleQueryStringPredicateTerminalContext extends SearchPredicateTerminalContext {

	/**
	 * Define the default operator as AND.
	 * <p>
	 * By default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document (OR operator).
	 * This will change the default behavior,
	 * making document match if <em>all</em> terms mentioned in the query string are present in the document.
	 *
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryStringPredicateTerminalContext withAndAsDefaultOperator();

	// TODO HSEARCH-3312 allow analyzer/normalizer override

}
