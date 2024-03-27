/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An extractor of values from a container.
 * <p>
 * Container extractors tell Hibernate Search how to extract values from object properties:
 * no extractor would mean using the property value directly,
 * a {@link BuiltinContainerExtractors#COLLECTION collection element extractor}
 * would extract each element of a collection,
 * a {@link BuiltinContainerExtractors#MAP_KEY map keys extractor}
 * would extract each key of a map,
 * etc.
 * @param <C> The type of containers this extractor can extract values from.
 * @param <V> The type of values extracted by this extractor.
 * @see ContainerExtractorPath
 * @see BuiltinContainerExtractors
 */
@Incubating
public interface ContainerExtractor<C, V> {

	/**
	 * @param container A container to extract values from.
	 * @param perValueProcessor A processor for values extracted from the container.
	 * @param target The target to pass to the {@code perValueProcessor}.
	 * @param context The context to pass to the {@code perValueProcessor}.
	 * @param extractionContext A context for use by the container extractor itself.
	 * @param <T> The type of the {@code target} of the {@code perValueProcessor},
	 * i.e. whatever it is supposed to push the result of its processing to.
	 * @param <C2> The type of the {@code context} of the {@code perValueProcessor},
	 * i.e. whatever information it needs that is independent from the target or value.
	 */
	<T, C2> void extract(C container, ValueProcessor<T, ? super V, C2> perValueProcessor, T target, C2 context,
			ContainerExtractionContext extractionContext);

	/**
	 * @return {@code true} if this extractor's {@link #extract(Object, ValueProcessor, Object, Object, ContainerExtractionContext)}
	 * method may call the consumer multiple times.
	 * {@code false} if it will always call the {@code consumer} either zero or one time for a given container.
	 */
	default boolean multiValued() {
		return true;
	}

}
