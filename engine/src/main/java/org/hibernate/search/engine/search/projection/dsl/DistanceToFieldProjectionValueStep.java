/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.List;

/**
 * The initial step in a "distance to field" projection definition,
 * where the projection (optionally) can be marked as multi-valued (returning Lists),
 * and where optional parameters can be set.
 * <p>
 * By default (if {@link #multi()} is not called), the projection is considered single-valued,
 * and its creation will fail if the field is multi-valued.
 *
 * @param <N> The next step if a method other than {@link #multi()} is called,
 * i.e. the return type of methods defined in {@link FieldProjectionOptionsStep}
 * when called directly on this object.
 * @param <T> The type of projected distances.
 */
public interface DistanceToFieldProjectionValueStep<N extends DistanceToFieldProjectionOptionsStep<?, T>, T>
		extends DistanceToFieldProjectionOptionsStep<N, T> {

	/**
	 * Defines the projection as multi-valued, i.e. returning {@code List<T>} instead of {@code T}.
	 * <p>
	 * Calling {@link #multi()} is mandatory for multi-valued fields,
	 * otherwise the projection will throw an exception upon creating the query.
	 *
	 * @return A new step to define optional parameters for the multi-valued projections.
	 */
	DistanceToFieldProjectionOptionsStep<?, List<T>> multi();

}
