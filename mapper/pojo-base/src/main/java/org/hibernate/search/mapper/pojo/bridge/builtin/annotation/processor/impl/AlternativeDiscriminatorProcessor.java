/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.annotation.processor.impl;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.AlternativeDiscriminator;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class AlternativeDiscriminatorProcessor implements PropertyMappingAnnotationProcessor<AlternativeDiscriminator> {

	@Override
	public void process(PropertyMappingStep mapping, AlternativeDiscriminator annotation,
			PropertyMappingAnnotationProcessorContext context) {
		mapping.marker( AlternativeBinder.alternativeDiscriminator()
				.id( context.toNullIfDefault( annotation.id(), "" ) ) );
	}
}
