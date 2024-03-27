/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

/**
 * The initial and final step in a "score" sort definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface ScoreSortOptionsStep<S extends ScoreSortOptionsStep<?>>
		extends SortFinalStep, SortThenStep, SortOrderStep<S> {
}
