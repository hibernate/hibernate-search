/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

/**
 * A property in the entity model annotated with a mapping annotation.
 *
 * @see MappingAnnotatedElement
 * @see PropertyMappingAnnotationProcessorContext#annotatedElement()
 */
public interface MappingAnnotatedProperty extends MappingAnnotatedElement {

	/**
	 * @return The name of the annotated property.
	 * In the case of a getter method, the name does not include the "get" prefix
	 * and the first character is lowercased.
	 */
	String name();

	/**
	 * @param extractorPath A container extractor path, possibly just {@link ContainerExtractorPath#defaultExtractors()}.
	 * @return The raw type of values one would obtain
	 * by using the given container extractor path to extract values from this property,
	 * or {@link Optional#empty()} if the container extractor path cannot be applied.
	 */
	Optional<Class<?>> javaClass(ContainerExtractorPath extractorPath);

}
