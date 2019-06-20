/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor;

import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

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
public interface ContainerExtractor<C, V> {

	/**
	 * @param container A container to extract values from.
	 * @return A stream of values extracted from the given container.
	 * The stream will be {@link Stream#close()} by the caller.
	 */
	Stream<V> extract(C container);

	/**
	 * @return {@code true} if this extractor's {@link #extract(Object)} method may return streams with more than one value.
	 * {@code false} if it will never return streams with more than one value.
	 */
	default boolean isMultiValued() {
		return true;
	}

}
