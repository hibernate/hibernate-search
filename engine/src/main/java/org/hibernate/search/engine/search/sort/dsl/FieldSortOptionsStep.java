/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

/**
 * The initial and final step in a "field" sort definition, where optional parameters can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortOptionsStep<
		SR,
		S extends FieldSortOptionsStep<SR, ?, PDF>,
		PDF extends TypedSearchPredicateFactory<SR>>
		extends FieldSortOptionsGenericStep<SR, Object, S, FieldSortMissingValueBehaviorStep<S>, PDF> {

}
