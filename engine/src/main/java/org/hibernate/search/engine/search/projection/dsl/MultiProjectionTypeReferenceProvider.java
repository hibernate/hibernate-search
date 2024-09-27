/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Defines the provider that can create {@link MultiProjectionTypeReference projection type references} based
 * on a container type {@code C} and container element type {@code V}.
 */
@Incubating
public interface MultiProjectionTypeReferenceProvider {
	/**
	 *
	 * @param containerType The type of the expected container.
	 * @param containerElementType The type of the container elements
	 * @return The projection type reference for a requested container/element types.
	 * @param <C> The type of the container.
	 * @param <V> The type of the container elements.
	 */
	<C, V> MultiProjectionTypeReference<C, V> multiProjectionTypeReference(Class<C> containerType,
			Class<V> containerElementType);
}
