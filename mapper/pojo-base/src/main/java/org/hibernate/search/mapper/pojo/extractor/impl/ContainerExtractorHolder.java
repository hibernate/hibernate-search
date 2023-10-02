/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;

public interface ContainerExtractorHolder<C, V> extends AutoCloseable {

	@Override
	void close();

	/**
	 * @param perValueProcessor A processor for values extracted from the container.
	 * @return A processor that accepts a container,
	 * extracts values from the given container and passes each value to the given
	 * {@code perValueProcessor}.
	 */
	<T, C2> ValueProcessor<T, C, C2> wrap(ValueProcessor<T, ? super V, C2> perValueProcessor);

	/**
	 * @return See {@link ContainerExtractor#multiValued()}.
	 */
	boolean multiValued();

	void appendToString(StringBuilder builder);
}
