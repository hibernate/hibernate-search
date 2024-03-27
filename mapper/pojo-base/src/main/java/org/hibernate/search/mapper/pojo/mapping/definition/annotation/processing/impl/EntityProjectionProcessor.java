/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.EntityProjectionBinder;

public final class EntityProjectionProcessor
		implements MethodParameterMappingAnnotationProcessor<EntityProjection> {

	@Override
	public void process(MethodParameterMappingStep mapping, EntityProjection annotation,
			MethodParameterMappingAnnotationProcessorContext context) {
		mapping.projection( EntityProjectionBinder.create() );
	}

}
