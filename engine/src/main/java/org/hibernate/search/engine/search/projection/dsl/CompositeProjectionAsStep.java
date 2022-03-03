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
 * The step in a "multi-step" composite projection definition
 * where one or more inner projections have been defined
 * and the result of the composite projection can be defined.
 */
public interface CompositeProjectionAsStep {

	/**
	 * Defines the result of the composite projection
	 * as {@link List} that will contain the results of inner projections defined so far, in order.
	 *
	 * @return The next DSL step.
	 */
	CompositeProjectionOptionsStep<?, List<?>> asList();

	/**
	 * Defines the result of the composite projection
	 * as the result of applying the given function to a {@link List} containing
	 * the results of inner projections defined so far, in order.
	 *
	 * @param transformer A function to transform the values of inner projections added so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionOptionsStep<?, V> asList(Function<List<?>, V> transformer);

}
