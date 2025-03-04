/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.metamodel.processor.model.BuiltInBridgeResolverTypes;
import org.hibernate.search.metamodel.processor.model.ProcessorPojoRawTypeModel;
import org.hibernate.search.metamodel.processor.model.ProcessorTypeOrdering;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

public class ProcessorPojoModelsBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final Map<Name, PojoRawTypeModel<?>> elementTypeModelCache = new HashMap<>();
	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();
	private final ProcessorIntrospectorContext context;
	private final PojoBootstrapIntrospector delegate;
	private final ProcessorTypeOrdering typeOrdering;

	public ProcessorPojoModelsBootstrapIntrospector(ProcessorIntrospectorContext context, PojoBootstrapIntrospector delegate) {
		this.context = context;
		this.delegate = delegate;
		this.typeOrdering = new ProcessorTypeOrdering( context.processorContext() );
	}

	@Override
	public <T> PojoRawTypeModel<T> typeModel(Class<T> clazz) {
		return delegate.typeModel( clazz );
	}

	@Override
	public PojoRawTypeModel<?> typeModel(String name) {
		TypeElement typeElement = context.typeElementsByName().get( name );
		if ( typeElement == null ) {
			try {
				typeElement = context.processorContext().elementUtils().getTypeElement( name );
			}
			catch (Exception e) {
				throw new IllegalArgumentException( name + " not found", e );
			}
		}
		return typeModel( typeElement );
	}

	public PojoRawTypeModel<?> typeModel(TypeElement typeElement) {
		Name qualifiedName = typeElement.getQualifiedName();
		return elementTypeModelCache.computeIfAbsent( qualifiedName,
				k -> {
					Optional<Class<?>> loadableType = BuiltInBridgeResolverTypes.loadableType( typeElement.asType() );
					if ( loadableType.isPresent() ) {
						return typeModel( loadableType.get() );
					}
					else {
						return new ProcessorPojoRawTypeModel<>( typeElement, context.processorContext(), this );
					}
				} );
	}

	@Override
	public ValueHandleFactory annotationValueHandleFactory() {
		return delegate.annotationValueHandleFactory();
	}

	public ProcessorTypeOrdering typeOrdering() {
		return typeOrdering;
	}
}
