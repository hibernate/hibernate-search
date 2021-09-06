/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
