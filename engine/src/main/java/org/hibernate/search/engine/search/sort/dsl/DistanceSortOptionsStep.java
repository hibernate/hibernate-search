/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The initial and final step in a "distance" sort definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 */
public interface DistanceSortOptionsStep<E, S extends DistanceSortOptionsStep<E, ?, PDF>, PDF extends SearchPredicateFactory<E>>
		extends SortFinalStep, SortThenStep<E>, SortOrderStep<S>, SortModeStep<S>, SortFilterStep<E, S, PDF> {

	/**
	 * Start describing the behavior of this sort when a document doesn't
	 * have any value for the targeted field.
	 *
	 * @return The next step.
	 */
	DistanceSortMissingValueBehaviorStep<S> missing();

}
