/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "terms" predicate definition where the terms to match can be set
 * (see the superinterface {@link TermsPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface TermsPredicateFieldMoreStep<
				S extends TermsPredicateFieldMoreStep<?, N>,
				N extends TermsPredicateOptionsStep<?>
		>
		extends TermsPredicateMatchingStep<N>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the terms predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link TermsPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 *
	 * @see TermsPredicateFieldStep#field(String)
	 */
	default S field(String absoluteFieldPath) {
		return fields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the terms predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link TermsPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 *
	 * @see TermsPredicateFieldStep#fields(String...)
	 */
	S fields(String... absoluteFieldPaths);

}
