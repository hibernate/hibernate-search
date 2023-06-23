/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "regexp" predicate definition where the pattern to match can be set
 * (see the superinterface {@link RegexpPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface RegexpPredicateFieldMoreStep<
		S extends RegexpPredicateFieldMoreStep<?, N>,
		N extends RegexpPredicateOptionsStep<?>>
		extends RegexpPredicateMatchingStep<N>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the regexp predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link RegexpPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see RegexpPredicateFieldStep#field(String)
	 */
	default S field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the regexp predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link RegexpPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see RegexpPredicateFieldStep#fields(String...)
	 */
	S fields(String... fieldPaths);

}
