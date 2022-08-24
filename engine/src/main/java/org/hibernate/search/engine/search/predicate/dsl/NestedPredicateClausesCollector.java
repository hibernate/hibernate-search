/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * An object where the <a href="SimpleBooleanPredicateClausesCollector.html#clauses">clauses</a>
 * of a {@link SearchPredicateFactory#nested(String) nested predicate} can be set.
 * <p>
 * The resulting nested predicate must match <em>all</em> inner clauses,
 * similarly to an {@link SearchPredicateFactory#and() "and" predicate}.
 */
public interface NestedPredicateClausesCollector<S extends NestedPredicateClausesCollector<?>>
		extends SimpleBooleanPredicateClausesCollector<S> {

}
