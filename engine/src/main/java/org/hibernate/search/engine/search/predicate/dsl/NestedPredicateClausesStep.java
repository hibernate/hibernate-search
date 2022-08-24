/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a nested predicate definition where
 * <a href="SimpleBooleanPredicateClausesCollector.html#clauses">clauses</a> can be added.
 * <p>
 * The resulting nested predicate must match <em>all</em> inner clauses,
 * similarly to an {@link SearchPredicateFactory#and() "and" predicate}.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface NestedPredicateClausesStep<S extends NestedPredicateClausesStep<?>>
		extends GenericSimpleBooleanPredicateClausesStep<S, NestedPredicateClausesCollector<?>>,
				NestedPredicateClausesCollector<NestedPredicateClausesCollector<?>> {

	// TODO HSEARCH-3090 add tuning methods, like the "score_mode" in Elasticsearch (avg, min, ...)

}
