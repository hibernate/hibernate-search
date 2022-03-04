/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

/**
 * The step in a "multi-step" composite projection definition
 * where a single inner projection has been defined
 * and the result of the composite projection can be defined.
 *
 * @param <V1> The type of values returned by the single inner projection.
 */
public interface CompositeProjectionFrom1AsStep<V1>
		extends CompositeProjectionAsStep {

	/**
	 * Defines the result of the composite projection
	 * as the result of applying the given function to the single inner projection defined so far.
	 *
	 * @param transformer A function to transform the values of inner projections defined so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionValueStep<?, V> as(Function<V1, V> transformer);

}
