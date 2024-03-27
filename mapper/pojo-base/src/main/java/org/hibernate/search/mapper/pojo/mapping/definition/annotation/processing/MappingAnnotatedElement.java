/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
