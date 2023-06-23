/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;


/**
 * The initial step in a "within" predicate definition, where the target field can be set.
 *
 * @param <N> The type of the next step.
 */
public interface SpatialWithinPredicateFieldStep<N extends SpatialWithinPredicateFieldMoreStep<?, ?>> {

	/**
	 * Target the given field in the "within" predicate.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	default N field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the "within" predicate.
	 * <p>
	 * Equivalent to {@link #field(String)} followed by multiple calls to
	 * {@link RangePredicateFieldMoreStep#field(String)},
	 * the only difference being that calls to {@link RangePredicateFieldMoreStep#boost(float)}
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

}
