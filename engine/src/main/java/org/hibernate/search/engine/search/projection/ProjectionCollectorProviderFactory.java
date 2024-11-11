/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Defines the factory that can create {@link ProjectionCollector.Provider projection collector providers} based
 * on a container type {@code R} and container element type {@code U}.
 */
@Incubating
public interface ProjectionCollectorProviderFactory {
	/**
	 *
	 * @param containerType The type of the expected container.
	 * Passing a {@code null} value as a container type will result in {@link ProjectionCollector#nullable() a nullable collector}
	 * being returned, i.e. a collector that does not wrap the value in any sort of container.
	 * @param containerElementType The type of the container elements
	 * @return The projection collector provider for a requested container/element types.
	 * @param <U> The type of values to collector after being transformed.
	 * @param <R> The type of the final result containing values of type {@code V}.
	 */
	<R, U> ProjectionCollector.Provider<U, R> projectionCollectorProvider(Class<R> containerType,
			Class<U> containerElementType);
}
