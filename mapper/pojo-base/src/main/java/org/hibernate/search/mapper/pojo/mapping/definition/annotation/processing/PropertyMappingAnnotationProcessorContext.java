/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

/**
 * The context passed to {@link PropertyMappingAnnotationProcessor#process(PropertyMappingStep, Annotation, PropertyMappingAnnotationProcessorContext)}.
 */
public interface PropertyMappingAnnotationProcessorContext extends MappingAnnotationProcessorContext {

	@Override
	MappingAnnotatedProperty annotatedElement();

}
