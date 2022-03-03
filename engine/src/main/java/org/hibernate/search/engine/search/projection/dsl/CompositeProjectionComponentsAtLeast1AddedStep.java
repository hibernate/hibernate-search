/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.List;
import java.util.function.Function;

/**
 * A step in a "multi-step" composite projection definition where 1 component or more have been added,
 * and more components can be added,
 * or the projection can be used as-is to project to a {@link List}.
 */
public interface CompositeProjectionComponentsAtLeast1AddedStep
		extends CompositeProjectionComponentsAddStep {

	/**
	 * Sets the projection to project to a {@link List}
	 * that will contain values for the projection components added so far, in order.
	 *
	 * @return The final step, where the projection can be retrieved.
	 */
	CompositeProjectionOptionsStep<?, List<?>> asList();

	/**
	 * Sets the given function as the way to transform the values of the projection components added so far,
	 * passed as a {@code List<?>}.
	 *
	 * @param transformer A function to transform the values of projection components added so far.
	 * @return The final step, where the projection can be retrieved.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionOptionsStep<?, V> transformList(Function<List<?>, V> transformer);

}
