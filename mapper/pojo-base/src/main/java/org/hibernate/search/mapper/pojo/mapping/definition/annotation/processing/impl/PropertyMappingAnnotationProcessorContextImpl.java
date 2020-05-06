/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedProperty;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

public class PropertyMappingAnnotationProcessorContextImpl
		extends AbstractMappingAnnotationProcessorContext
		implements PropertyMappingAnnotationProcessorContext, MappingAnnotatedProperty {
	private final PojoPropertyModel<?> propertyModel;

	public PropertyMappingAnnotationProcessorContextImpl(PojoPropertyModel<?> propertyModel) {
		this.propertyModel = propertyModel;
	}

	@Override
	public MappingAnnotatedProperty annotatedElement() {
		return this; // Not a lot to implement, so we just implement everything in the same class
	}

	@Override
	public Class<?> javaClass() {
		return propertyModel.getTypeModel().getRawType().getTypeIdentifier().getJavaClass();
	}

	@Override
	public String name() {
		return propertyModel.getName();
	}

	@Override
	public Stream<Annotation> allAnnotations() {
		return propertyModel.getAnnotations();
	}
}
