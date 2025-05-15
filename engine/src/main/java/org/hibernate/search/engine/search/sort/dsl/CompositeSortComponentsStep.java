/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

/**
 * The initial and final step in a composite sort definition, where sort elements can be added.
 * <p>
 * This is only used in "explicit" composite sorts,
 * for example when calling {@link SearchSortFactory#composite()},
 * but not in "implicit" composite sorts such as when calling {@link SortThenStep#then()}.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface CompositeSortComponentsStep<SR, S extends CompositeSortComponentsStep<SR, ?>>
		extends CompositeSortOptionsCollector<S>, SortFinalStep, SortThenStep<SR> {

}
