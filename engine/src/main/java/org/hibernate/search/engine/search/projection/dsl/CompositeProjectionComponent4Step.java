/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.util.common.function.TriFunction;

/**
 * The fourth step in a "multi-step" composite projection definition,
 * where 3 projection components have been added and the fourth component can be added,
 * or the projection can be used as-is.
 *
 * @param <V1> The type of values returned by the first projection component.
 * @param <V2> The type of values returned by the second projection component.
 * @param <V3> The type of values returned by the third projection component.
 */
public interface CompositeProjectionComponent4Step<V1, V2, V3>
		extends CompositeProjectionComponentsAtLeast1AddedStep {

	/**
	 * Sets the given function as the way to transform the values of projection components added so far,
	 * combining them into a single value.
	 *
	 * @param transformer A function to transform the values of projection components added so far.
	 * @return The final step, where the projection can be retrieved.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionOptionsStep<?, V> transform(TriFunction<V1, V2, V3, V> transformer);

}
