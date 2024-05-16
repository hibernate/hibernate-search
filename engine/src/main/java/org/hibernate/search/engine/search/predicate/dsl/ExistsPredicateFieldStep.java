/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.TypedFieldReference;

/**
 * The initial step in an "exists" predicate definition, where the target field can be set.
 *
 * @param <N> The type of the next step.
 */
public interface ExistsPredicateFieldStep<N extends ExistsPredicateOptionsStep<?>> {

	/**
	 * Target the given field in the "exists" predicate.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	N field(String fieldPath);

	/**
	 * Target the given field in the "exists" predicate.
	 *
	 * @param field The field reference representing a <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	default N field(TypedFieldReference<?> field) {
		return field( field.absolutePath() );
	}

}
