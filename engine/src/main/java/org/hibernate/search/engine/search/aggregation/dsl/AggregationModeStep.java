/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import org.hibernate.search.engine.search.common.MultiValue;

/**
 * The step in a aggregation definition where the {@link MultiValue} can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 */
public interface AggregationModeStep<S> {

	/**
	 * Start describing the behavior of this aggregation when a document has multiple values for the targeted field.
	 *
	 * @param mode The mode.
	 * @return {@code this}, for method chaining.
	 */
	S mode(MultiValue mode);

}
