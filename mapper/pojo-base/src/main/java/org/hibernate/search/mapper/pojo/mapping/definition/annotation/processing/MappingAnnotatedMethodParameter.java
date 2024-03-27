/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.util.Optional;

/**
 * A method parameter in the entity model annotated with a mapping annotation.
 *
 * @see MappingAnnotatedElement
 * @see PropertyMappingAnnotationProcessorContext#annotatedElement()
 */
public interface MappingAnnotatedMethodParameter extends MappingAnnotatedElement {

	/**
	 * @return An optional containing the name of the method parameter, or {@link Optional#empty()} if unavailable.
	 */
	Optional<String> name();

}
