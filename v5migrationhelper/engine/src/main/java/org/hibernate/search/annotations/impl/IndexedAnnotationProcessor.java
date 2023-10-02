/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations.impl;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

@Deprecated
public class IndexedAnnotationProcessor implements TypeMappingAnnotationProcessor<Indexed> {
	@Override
	public void process(TypeMappingStep mapping, Indexed annotation, TypeMappingAnnotationProcessorContext context) {
		String indexName = context.toNullIfDefault( annotation.index(), "" );
		mapping.indexed().index( indexName );
	}
}
