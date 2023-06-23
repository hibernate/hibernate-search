/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "simple query string" predicate definition where the query string to match can be set
 * (see the superinterface {@link SimpleQueryStringPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface SimpleQueryStringPredicateFieldMoreStep<
		S extends SimpleQueryStringPredicateFieldMoreStep<?, N>,
		N extends SimpleQueryStringPredicateOptionsStep<?>>
		extends SimpleQueryStringPredicateMatchingStep<N>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the simple query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link SimpleQueryStringPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see SimpleQueryStringPredicateFieldStep#field(String)
	 */
	default S field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the simple query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link SimpleQueryStringPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see SimpleQueryStringPredicateFieldStep#fields(String...)
	 */
	S fields(String... fieldPaths);

}
