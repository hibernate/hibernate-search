/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.RangePredicateFieldReference;

/**
 * The step in a "range" predicate definition where the limits of the range to match can be set
 * (see the superinterface {@link RangePredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 * @param <V> The type representing the fields.
 * @param <T> The type of the boundary values.
 */
public interface RangePredicateFieldMoreGenericStep<
		SR,
		S extends RangePredicateFieldMoreGenericStep<SR, ?, N, V, T>,
		N extends RangePredicateOptionsStep<?>,
		V,
		T>
		extends RangePredicateMatchingGenericStep<T, N>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the range predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link RangePredicateFieldStep#field(RangePredicateFieldReference)} for more information about targeting fields.
	 *
	 * @param field The field with a <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see RangePredicateFieldStep#field(RangePredicateFieldReference)
	 */
	S field(V field);

	/**
	 * Target the given fields in the range predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link RangePredicateFieldStep#fields(RangePredicateFieldReference...)} for more information about targeting fields.
	 *
	 * @param fields The fields with <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see RangePredicateFieldStep#fields(RangePredicateFieldReference...)
	 */
	@SuppressWarnings("unchecked")
	S fields(V... fields);

}
