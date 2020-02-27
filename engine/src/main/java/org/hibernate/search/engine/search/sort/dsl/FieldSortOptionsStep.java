/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.hibernate.search.engine.search.common.MultiValue;

/**
 * The initial and final step in a "field" sort definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortOptionsStep<S extends FieldSortOptionsStep<?>>
		extends SortFinalStep, SortThenStep, SortOrderStep<S> {

	/**
	 * Start describing the behavior of this sort when a document doesn't have any value for the targeted field.
	 *
	 * @return The next step.
	 */
	FieldSortMissingValueBehaviorStep<S> missing();

	/**
	 * Start describing the behavior of this sort when a document do have
	 * mode values for the targeted field.
	 *
	 * @param mode The mode.
	 * @return {@code this}, for method chaining.
	 */
	S mode(MultiValue mode);
}
