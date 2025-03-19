/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.mapping.impl;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.metamodel.processor.model.impl.BuiltInBridgeResolverTypes;
import org.hibernate.search.metamodel.processor.model.impl.ProcessorPojoRawTypeModel;
import org.hibernate.search.metamodel.processor.model.impl.ProcessorTypeOrdering;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

public class ProcessorPojoModelsBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final Map<Name, PojoRawTypeModel<?>> elementTypeModelCache = new HashMap<>();
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
		TypeElement typeElement = context.typeElementsByName( name );
		if ( typeElement == null ) {
			try {
				typeElement = context.elementUtils().getTypeElement( name );
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
					Optional<Class<?>> loadableType =
							BuiltInBridgeResolverTypes.loadableType( typeElement.asType(), context.typeUtils() );
					if ( loadableType.isPresent() ) {
						return typeModel( loadableType.get() );
					}
					else {
						return new ProcessorPojoRawTypeModel<>( typeElement, context.processorContext(), this );
					}
				} );
	}

	public PojoRawTypeModel<?> typeModel(TypeMirror typeMirror) {
		if ( typeMirror instanceof PrimitiveType primitiveType ) {
			// box the primitive so it's easier to deal with them later:
			typeMirror = context.typeUtils().boxedClass( primitiveType ).asType();
		}
		Optional<Class<?>> loadableType =
				BuiltInBridgeResolverTypes.loadableType( typeMirror, context.processorContext().typeUtils() );
		if ( loadableType.isPresent() ) {
			return typeModel( loadableType.get() );
		}
		else {
			return new ProcessorPojoRawTypeModel<>( typeMirror, context.processorContext(), this );
		}
	}

	@Override
	public ValueHandleFactory annotationValueHandleFactory() {
		return delegate.annotationValueHandleFactory();
	}

	public ProcessorTypeOrdering typeOrdering() {
		return typeOrdering;
	}
}
