/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The initial and final step in a "field" sort definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortOptionsStep<S extends FieldSortOptionsStep<?, PDF>, PDF extends SearchPredicateFactory>
		extends SortFinalStep, SortThenStep, SortOrderStep<S>, SortModeStep<S>, SortFilterStep<S, PDF> {

	/**
	 * Start describing the behavior of this sort when a document doesn't
	 * have any value for the targeted field.
	 *
	 * @return The next step.
	 */
	FieldSortMissingValueBehaviorStep<S> missing();

}
