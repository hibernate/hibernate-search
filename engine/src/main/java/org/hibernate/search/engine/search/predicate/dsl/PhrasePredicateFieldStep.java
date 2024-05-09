/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.PhrasePredicateFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial step in a "phrase" predicate definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <N> The type of the next step.
 */
public interface PhrasePredicateFieldStep<SR, N extends PhrasePredicateFieldMoreStep<SR, ?, ?>> {

	/**
	 * Target the given field in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
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
	 * Target the given fields in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #field(String)} followed by multiple calls to
	 * {@link PhrasePredicateFieldMoreStep#field(String)},
	 * the only difference being that calls to {@link PhrasePredicateFieldMoreStep#boost(float)}
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
	 * Target the given field in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * Please refer to the reference documentation for more information.
	 *
	 * @param field The field reference representing a <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#field(PhrasePredicateFieldReference)
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default N field(PhrasePredicateFieldReference<SR, ?> field) {
		return fields( field );
	}

	/**
	 * Target the given fields in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #field(PhrasePredicateFieldReference)} followed by multiple calls to
	 * {@link PhrasePredicateFieldMoreStep#field(PhrasePredicateFieldReference)},
	 * the only difference being that calls to {@link PhrasePredicateFieldMoreStep#boost(float)}
	 * and other field-specific settings on the returned step will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param fields The field references representing <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#fields(PhrasePredicateFieldReference...)
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default N fields(PhrasePredicateFieldReference<SR, ?>... fields) {
		String[] paths = new String[fields.length];
		for ( int i = 0; i < fields.length; i++ ) {
			paths[i] = fields[i].absolutePath();
		}
		return fields( paths );
	}

}
