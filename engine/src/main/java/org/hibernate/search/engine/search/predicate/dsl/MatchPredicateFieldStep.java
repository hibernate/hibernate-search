/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;

/**
 * The initial step in a "match" predicate definition, where the target field can be set.
 */
public interface MatchPredicateFieldStep<SR, N extends MatchPredicateFieldMoreStep<?, ?>> {

	/**
	 * Target the given field in the match predicate.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * Please refer to the reference documentation for more information.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	default N field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the match predicate.
	 * <p>
	 * Equivalent to {@link #field(String)} followed by multiple calls to
	 * {@link MatchPredicateFieldMoreStep#field(Object)},
	 * the only difference being that calls to {@link MatchPredicateFieldMoreStep#boost(float)}
	 * and other field-specific settings on the returned step will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see #field(String)
	 */
	N fields(String... fieldPaths);

	/**
	 * Target the given field in the match predicate.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * Please refer to the reference documentation for more information.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchPredicateFactory.html#field-references">definition</a> of the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	@SuppressWarnings("unchecked")
	default <T> MatchPredicateFieldMoreGenericStep<?, ?, T, MatchPredicateFieldReference<? super SR, T>> field(
			MatchPredicateFieldReference<? super SR, T> fieldReference) {
		return fields( fieldReference );
	}

	/**
	 * Target the given fields in the match predicate.
	 * <p>
	 * Equivalent to {@link #field(String)} followed by multiple calls to
	 * {@link MatchPredicateFieldMoreStep#field(Object)},
	 * the only difference being that calls to {@link MatchPredicateFieldMoreStep#boost(float)}
	 * and other field-specific settings on the returned step will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param fieldReferences The field references representing <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see #field(MatchPredicateFieldReference)
	 */
	@SuppressWarnings("unchecked")
	<T> MatchPredicateFieldMoreGenericStep<?, ?, T, MatchPredicateFieldReference<? super SR, T>> fields(
			MatchPredicateFieldReference<? super SR, T>... fieldReferences);
}
