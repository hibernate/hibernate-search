/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.SpatialPredicateFieldReference;

/**
 * The step in a "within" predicate definition where the area to match can be set
 * (see the superinterface {@link SpatialWithinPredicateAreaStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface SpatialWithinPredicateFieldMoreStep<
		SR,
		S extends SpatialWithinPredicateFieldMoreStep<SR, ?, N>,
		N extends SpatialWithinPredicateOptionsStep<?>>
		extends SpatialWithinPredicateAreaStep<N>,
		MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see SpatialWithinPredicateFieldStep#field(String)
	 */
	default S field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see SpatialWithinPredicateFieldStep#fields(String...)
	 */
	S fields(String... fieldPaths);

	/**
	 * Target the given field in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchPredicateFactory.html#field-references">definition</a> of the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see SpatialWithinPredicateFieldStep#field(String)
	 */
	@SuppressWarnings("unchecked")
	default S field(SpatialPredicateFieldReference<? super SR> fieldReference) {
		return fields( fieldReference );
	}

	/**
	 * Target the given fields in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fieldReferences The field references representing <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see SpatialWithinPredicateFieldStep#fields(String...)
	 */
	@SuppressWarnings("unchecked")
	S fields(SpatialPredicateFieldReference<? super SR>... fieldReferences);
}
