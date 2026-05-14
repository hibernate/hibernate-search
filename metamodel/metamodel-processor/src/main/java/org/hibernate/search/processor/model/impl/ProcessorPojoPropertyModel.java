/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.hibernate.accessor.HibernateAccessorValueReader;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.processor.impl.HibernateSearchMetamodelProcessorContext;
import org.hibernate.search.processor.mapping.impl.ProcessorPojoModelsBootstrapIntrospector;

public class ProcessorPojoPropertyModel<T> implements PojoPropertyModel<T> {

	private final Element element;
	private final HibernateSearchMetamodelProcessorContext context;
	private final ProcessorPojoModelsBootstrapIntrospector introspector;

	private final String propertyName;
	private final TypeMirror propertyType;

	public ProcessorPojoPropertyModel(VariableElement element, String propertyName,
			HibernateSearchMetamodelProcessorContext context,
			ProcessorPojoModelsBootstrapIntrospector introspector) {
		this.element = element;
		this.context = context;
		this.introspector = introspector;

		this.propertyName = propertyName;
		this.propertyType = element.asType();
	}

	public ProcessorPojoPropertyModel(ExecutableElement element, String propertyName,
			HibernateSearchMetamodelProcessorContext context,
			ProcessorPojoModelsBootstrapIntrospector introspector) {
		this.element = element;
		this.context = context;
		this.introspector = introspector;

		this.propertyName = propertyName;
		this.propertyType = element.getReturnType();
	}

	@Override
	public String name() {
		return propertyName;
	}

	@Override
	public Stream<Annotation> annotations() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PojoTypeModel<T> typeModel() {
		return (PojoTypeModel<T>) introspector.typeModel( propertyType );
	}

	@SuppressWarnings("unchecked")
	@Override
	public HibernateAccessorValueReader<T> handle() {
		return (HibernateAccessorValueReader<T>) ProcessorValueReadHandle.INSTANCE;
	}

	private static class ProcessorValueReadHandle<T> implements HibernateAccessorValueReader<T> {
		static final ProcessorValueReadHandle<?> INSTANCE = new ProcessorValueReadHandle<>();

		@Override
		public T get(Object instance) {
			throw new UnsupportedOperationException();
		}
	}
}
