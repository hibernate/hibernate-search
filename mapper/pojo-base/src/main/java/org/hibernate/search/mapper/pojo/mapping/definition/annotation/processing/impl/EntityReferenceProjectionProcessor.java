/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityReferenceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.EntityReferenceProjectionBinder;

public final class EntityReferenceProjectionProcessor
		implements MethodParameterMappingAnnotationProcessor<EntityReferenceProjection> {

	@Override
	public void process(MethodParameterMappingStep mapping, EntityReferenceProjection annotation,
			MethodParameterMappingAnnotationProcessorContext context) {
		mapping.projection( EntityReferenceProjectionBinder.create() );
	}

}
