/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "wildcard" predicate definition where the pattern to match can be set
 * (see the superinterface {@link WildcardPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface WildcardPredicateFieldMoreStep<
		S extends WildcardPredicateFieldMoreStep<?, N>,
		N extends WildcardPredicateOptionsStep<?>>
		extends WildcardPredicateMatchingStep<N>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the wildcard predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link WildcardPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see WildcardPredicateFieldStep#field(String)
	 */
	default S field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the wildcard predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link WildcardPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see WildcardPredicateFieldStep#fields(String...)
	 */
	S fields(String... fieldPaths);

}
