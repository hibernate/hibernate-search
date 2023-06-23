/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedType;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;
import org.hibernate.search.util.common.reporting.EventContext;

public class TypeMappingAnnotationProcessorContextImpl
		extends AbstractMappingAnnotationProcessorContext
		implements TypeMappingAnnotationProcessorContext, MappingAnnotatedType {
	private final PojoRawTypeModel<?> typeModel;
	private final Annotation annotation;

	public TypeMappingAnnotationProcessorContextImpl(PojoRawTypeModel<?> typeModel, Annotation annotation,
			AnnotationHelper annotationHelper) {
		super( annotationHelper );
		this.typeModel = typeModel;
		this.annotation = annotation;
	}

	@Override
	public MappingAnnotatedType annotatedElement() {
		return this; // Not a lot to implement, so we just implement everything in the same class
	}

	@Override
	public EventContext eventContext() {
		return PojoEventContexts.fromType( typeModel )
				.append( PojoEventContexts.fromAnnotation( annotation ) );
	}

	@Override
	public Class<?> javaClass() {
		return typeModel.typeIdentifier().javaClass();
	}

	@Override
	public Stream<Annotation> allAnnotations() {
		return typeModel.annotations().flatMap( annotationHelper::expandRepeatableContainingAnnotation );
	}
}
