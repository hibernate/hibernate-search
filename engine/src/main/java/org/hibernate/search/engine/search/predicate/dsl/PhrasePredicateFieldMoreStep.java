/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.TypedFieldReference;

/**
 * The step in a "phrase" predicate definition where the phrase to match can be set
 * (see the superinterface {@link PhrasePredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface PhrasePredicateFieldMoreStep<
		S extends PhrasePredicateFieldMoreStep<?, N>,
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
	 * See {@link PhrasePredicateFieldStep#field(TypedFieldReference)} for more information on targeted fields.
	 *
	 * @param field The field reference representing a <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#field(TypedFieldReference)
	 */
	default S field(TypedFieldReference<?> field) {
		return fields( field );
	}

	/**
	 * Target the given fields in the phrase predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link PhrasePredicateFieldStep#fields(TypedFieldReference...)} for more information on targeted fields.
	 *
	 * @param fields The field references representing <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see PhrasePredicateFieldStep#fields(TypedFieldReference...)
	 */
	default S fields(TypedFieldReference<?>... fields) {
		String[] paths = new String[fields.length];
		for ( int i = 0; i < fields.length; i++ ) {
			paths[i] = fields[i].absolutePath();
		}
		return fields( paths );
	}

}
