/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.model.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.metamodel.processor.impl.HibernateSearchMetamodelProcessorContext;
import org.hibernate.search.metamodel.processor.mapping.impl.ProcessorPojoModelsBootstrapIntrospector;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

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
		// todo need to handle enums
		//  need to handle geo point subtypes
		//		Optional<Class<?>> loadableType = BuiltInBridgeResolverTypes.loadableType( propertyType, context.typeUtils() );
		//		if ( loadableType.isPresent() ) {
		//			return (PojoTypeModel<T>) introspector.typeModel( loadableType.get() );
		//		}
		//		else {
		return (PojoTypeModel<T>) introspector.typeModel( propertyType );
		//		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ValueReadHandle<T> handle() {
		return (ValueReadHandle<T>) ProcessorValueReadHandle.INSTANCE;
	}

	private static class ProcessorValueReadHandle<T> implements ValueReadHandle<T> {
		static final ProcessorValueReadHandle<?> INSTANCE = new ProcessorValueReadHandle<>();

		@Override
		public T get(Object thiz) {
			throw new UnsupportedOperationException();
		}
	}
}
