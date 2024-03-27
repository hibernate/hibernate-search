/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.ObjectProjectionBinder;

public final class ObjectProjectionProcessor
		implements MethodParameterMappingAnnotationProcessor<ObjectProjection> {

	@Override
	public void process(MethodParameterMappingStep mapping, ObjectProjection annotation,
			MethodParameterMappingAnnotationProcessorContext context) {
		mapping.projection( ObjectProjectionBinder.create( context.toNullIfDefault( annotation.path(), "" ) )
				.filter( new TreeFilterDefinition(
						context.toNullIfDefault( annotation.includeDepth(), -1 ),
						MappingAnnotationProcessorUtils.cleanUpPaths( annotation.includePaths() ),
						MappingAnnotationProcessorUtils.cleanUpPaths( annotation.excludePaths() )
				) ) );
	}

}
