/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations.impl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

@Deprecated
public class DocumentIdAnnotationProcessor implements PropertyMappingAnnotationProcessor<DocumentId> {
	@Override
	public void process(PropertyMappingStep mapping, DocumentId annotation,
			PropertyMappingAnnotationProcessorContext context) {
		mapping.documentId();
	}
}
