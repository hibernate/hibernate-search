/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Defines the factory that can create {@link ProjectionAccumulator.Provider projection accumulator providers} based
 * on a container type {@code R} and container element type {@code U}.
 */
@Incubating
public interface ProjectionAccumulatorProviderFactory {
	/**
	 *
	 * @param containerType The type of the expected container.
	 * Passing a {@code null} value as a container type will result in {@link ProjectionAccumulator#nullable() a nullable accumulator}
	 * being returned, i.e. an accumulator that does not wrap the value in any sort of container.
	 * @param containerElementType The type of the container elements
	 * @return The projection accumulator provider for a requested container/element types.
	 * @param <U> The type of values to accumulate after being transformed.
	 * @param <R> The type of the final result containing values of type {@code V}.
	 */
	<R, U> ProjectionAccumulator.Provider<U, R> projectionAccumulatorProvider(Class<R> containerType,
			Class<U> containerElementType);
}
