/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

/**
 * An element in the entity model annotated with a mapping annotation.
 *
 * @see MappingAnnotatedType
 * @see MappingAnnotatedProperty
 */
public interface MappingAnnotatedElement {

	/**
	 * @return The Java class corresponding to the raw type of the annotated element.
	 */
	Class<?> javaClass();

	/**
	 * @return All annotations declared on the annotated element.
	 */
	Stream<Annotation> allAnnotations();

}
