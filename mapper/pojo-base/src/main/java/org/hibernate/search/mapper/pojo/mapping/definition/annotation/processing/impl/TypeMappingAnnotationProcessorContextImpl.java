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

public class TypeMappingAnnotationProcessorContextImpl
		extends AbstractMappingAnnotationProcessorContext
		implements TypeMappingAnnotationProcessorContext, MappingAnnotatedType {
	private final PojoRawTypeModel<?> typeModel;

	public TypeMappingAnnotationProcessorContextImpl(PojoRawTypeModel<?> typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public MappingAnnotatedType annotatedElement() {
		return this; // Not a lot to implement, so we just implement everything in the same class
	}

	@Override
	public Class<?> javaClass() {
		return typeModel.typeIdentifier().javaClass();
	}

	@Override
	public Stream<Annotation> allAnnotations() {
		return typeModel.annotations();
	}
}
