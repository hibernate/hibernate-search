/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

/**
 * The initial and final step in a "score" sort definition, where optional parameters can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface ScoreSortOptionsStep<SR, S extends ScoreSortOptionsStep<SR, ?>>
		extends SortFinalStep, SortThenStep<SR>, SortOrderStep<S> {
}
