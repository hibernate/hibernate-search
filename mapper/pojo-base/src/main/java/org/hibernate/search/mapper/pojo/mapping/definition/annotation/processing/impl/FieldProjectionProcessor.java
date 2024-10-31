/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.FieldProjectionBinder;


public final class FieldProjectionProcessor
		implements MethodParameterMappingAnnotationProcessor<FieldProjection> {
	@SuppressWarnings({ "deprecation", "removal" })
	@Override
	public void process(MethodParameterMappingStep mapping, FieldProjection annotation,
			MethodParameterMappingAnnotationProcessorContext context) {
		ValueModel valueModel = annotation.valueModel();
		if ( !org.hibernate.search.engine.search.common.ValueConvert.DEFAULT.equals( annotation.convert() ) ) {
			if ( !ValueModel.DEFAULT.equals( valueModel ) ) {
				throw MappingLog.INSTANCE.usingNonDefaultValueConvertAndValueModelNotAllowed(
						valueModel.name(),
						annotation.convert().name(),
						context.eventContext()
				);
			}
			valueModel = org.hibernate.search.engine.search.common.ValueConvert.toValueModel( annotation.convert() );
		}

		mapping.projection( FieldProjectionBinder.create( context.toNullIfDefault( annotation.path(), "" ) )
				.valueModel( valueModel ) );
	}

}
