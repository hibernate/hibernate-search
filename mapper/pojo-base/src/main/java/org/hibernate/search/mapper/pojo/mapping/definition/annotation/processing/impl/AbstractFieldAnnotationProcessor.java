/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

abstract class AbstractFieldAnnotationProcessor<A extends Annotation> extends AbstractBaseFieldAnnotationProcessor<A> {

	@Override
	final PropertyMappingFieldOptionsStep<?> initBaseFieldMappingContext(PropertyMappingAnnotationProcessorContext context,
			PropertyMappingStep mappingContext, A annotation, String fieldName) {
		PropertyMappingFieldOptionsStep<?> fieldContext = initFieldMappingContext( mappingContext, annotation, fieldName );

		ContainerExtractorPath extractorPath = context.toContainerExtractorPath( getExtraction( annotation ) );
		fieldContext.extractors( extractorPath );

		return fieldContext;
	}

	abstract PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			A annotation, String fieldName);

	abstract ContainerExtraction getExtraction(A annotation);
}
