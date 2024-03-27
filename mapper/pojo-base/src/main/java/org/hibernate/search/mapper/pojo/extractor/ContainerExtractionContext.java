/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A handler passed to {@link ContainerExtractor#extract(Object, ValueProcessor, Object, Object, ContainerExtractionContext)}.
 *
 * @see ContainerExtractor
 */
@Incubating
public interface ContainerExtractionContext {

	/**
	 * Propagates (rethrows) a {@link RuntimeException} thrown while extracting elements from a container,
	 * or ignores it so that the container is assumed empty.
	 *
	 * @param exception A {@link RuntimeException} thrown while extracting elements from a container.
	 */
	void propagateOrIgnoreContainerExtractionException(RuntimeException exception);

}
