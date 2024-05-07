/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "match" predicate definition where the value to match can be set
 * (see the superinterface {@link MatchPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 * @param <T> The type of the match value.
 * @param <V> The type representing the fields.
 */
public interface MatchPredicateFieldMoreGenericStep<
		E,
		S extends MatchPredicateFieldMoreGenericStep<E, ?, N, T, V>,
		N extends MatchPredicateOptionsStep<?>,
		T,
		V>
		extends MatchPredicateMatchingGenericStep<N, T>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the match predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link MatchPredicateFieldStep#field(String)} for more information about targeting fields.
	 *
	 * @param field The field with a <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see MatchPredicateFieldStep#field(String)
	 */
	S field(V field);

	/**
	 * Target the given fields in the match predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link MatchPredicateFieldStep#fields(String...)} for more information about targeting fields.
	 *
	 * @param fieldPaths The fields with <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see MatchPredicateFieldStep#fields(String...)
	 */
	@SuppressWarnings("unchecked")
	S fields(V... fieldPaths);

}
