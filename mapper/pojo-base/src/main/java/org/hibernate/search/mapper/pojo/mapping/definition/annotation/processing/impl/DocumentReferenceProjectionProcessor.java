/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentReferenceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.DocumentReferenceProjectionBinder;

public final class DocumentReferenceProjectionProcessor
		implements MethodParameterMappingAnnotationProcessor<DocumentReferenceProjection> {

	@Override
	public void process(MethodParameterMappingStep mapping, DocumentReferenceProjection annotation,
			MethodParameterMappingAnnotationProcessorContext context) {
		mapping.projection( DocumentReferenceProjectionBinder.create() );
	}

}
