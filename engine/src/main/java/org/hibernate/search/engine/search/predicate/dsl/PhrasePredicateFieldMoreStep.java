/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.PhrasePredicateFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a "phrase" predicate definition where the phrase to match can be set
 * (see the superinterface {@link PhrasePredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface PhrasePredicateFieldMoreStep<
		SR,
		S extends PhrasePredicateFieldMoreStep<SR, ?, N>,
		N extends PhrasePredicateOptionsStep<?>>
		extends PhrasePredicateMatchingStep<N>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the phrase predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link PhrasePredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#field(String)
	 */
	default S field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the phrase predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link PhrasePredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#fields(String...)
	 */
	S fields(String... fieldPaths);

	/**
	 * Target the given field in the phrase predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link PhrasePredicateFieldStep#field(PhrasePredicateFieldReference)} for more information on targeted fields.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchPredicateFactory.html#field-references">definition</a> of the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#field(PhrasePredicateFieldReference)
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default S field(PhrasePredicateFieldReference<? super SR, ?> fieldReference) {
		return fields( fieldReference );
	}

	/**
	 * Target the given fields in the phrase predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link PhrasePredicateFieldStep#fields(PhrasePredicateFieldReference...)} for more information on targeted fields.
	 *
	 * @param fields The field references representing <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#fields(PhrasePredicateFieldReference...)
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default S fields(PhrasePredicateFieldReference<? super SR, ?>... fields) {
		String[] paths = new String[fields.length];
		for ( int i = 0; i < fields.length; i++ ) {
			paths[i] = fields[i].absolutePath();
		}
		return fields( paths );
	}

}
