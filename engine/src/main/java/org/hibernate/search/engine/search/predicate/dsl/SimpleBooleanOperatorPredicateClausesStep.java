/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial and final step in a "simple boolean operator predicate" definition,
 * where <a href="SimpleBooleanOperatorPredicateClausesCollector.html#clauses">clauses</a>
 * can be added and options can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface SimpleBooleanOperatorPredicateClausesStep<S extends SimpleBooleanOperatorPredicateClausesStep<?>>
		extends GenericSimpleBooleanOperatorPredicateClausesStep<S, SimpleBooleanOperatorPredicateClausesCollector<?>>,
		SimpleBooleanOperatorPredicateOptionsStep<S> {

}
