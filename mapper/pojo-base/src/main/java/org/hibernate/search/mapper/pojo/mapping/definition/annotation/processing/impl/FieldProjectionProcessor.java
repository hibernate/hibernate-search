/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.FieldProjectionBinder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class FieldProjectionProcessor
		implements MethodParameterMappingAnnotationProcessor<FieldProjection> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@SuppressWarnings("deprecation")
	@Override
	public void process(MethodParameterMappingStep mapping, FieldProjection annotation,
			MethodParameterMappingAnnotationProcessorContext context) {

		if ( org.hibernate.search.engine.search.common.ValueConvert.NO.equals( annotation.convert() ) ) {
			throw log.usingNonDefaultValueConvertNotAllowed( context.eventContext() );
		}
		mapping.projection( FieldProjectionBinder.create( context.toNullIfDefault( annotation.path(), "" ) )
				.valueModel( annotation.valueModel() ) );
	}

}
