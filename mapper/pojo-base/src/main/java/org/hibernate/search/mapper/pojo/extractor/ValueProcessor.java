/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A processor of values extracted from a container.
 *
 * @param <T> The type of the {@code target} of this processor,
 * i.e. whatever it is supposed to push the result of its processing to.
 * @param <V> The type of values processed by this processor.
 * @param <C> The type of the {@code context} of this processor,
 * i.e. whatever information it needs that is independent from the target or value.
 * @see ContainerExtractor#extract(Object, ValueProcessor, Object, Object, ContainerExtractionContext)
 */
@Incubating
public interface ValueProcessor<T, V, C> {

	/**
	 * @param target The {@code target} passed to
	 * {@link ContainerExtractor#extract(Object, ValueProcessor, Object, Object, ContainerExtractionContext)}.
	 * @param value The value to process.
	 * @param context The {@code context} passed to
	 * {@link ContainerExtractor#extract(Object, ValueProcessor, Object, Object, ContainerExtractionContext)}.
	 * @param extractionContext The {@code extractionContext} for use by the container extractor(s), if any.
	 */
	void process(T target, V value, C context, ContainerExtractionContext extractionContext);

}
