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
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortOptionsGenericStep<
		SR,
		T,
		S extends FieldSortOptionsGenericStep<SR, T, ?, ?, PDF>,
		N extends FieldSortMissingValueBehaviorGenericStep<T, S>,
		PDF extends SearchPredicateFactory<SR>>
		extends SortFinalStep, SortThenStep<SR>, SortOrderStep<S>, SortModeStep<S>, SortFilterStep<SR, S, PDF> {

	/**
	 * Start describing the behavior of this sort when a document doesn't
	 * have any value for the targeted field.
	 *
	 * @return The next step.
	 */
	N missing();

}
