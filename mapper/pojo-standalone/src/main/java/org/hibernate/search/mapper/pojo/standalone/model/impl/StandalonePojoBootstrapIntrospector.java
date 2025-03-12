/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.model.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.model.models.spi.AbstractPojoModelsBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.models.spi.PojoSimpleModelsRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.MappingLog;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.reflect.spi.ValueReadWriteHandle;

import org.jboss.jandex.IndexView;

/**
 * A very simple introspector for Pojo mapping in standalone mode (without Hibernate ORM).
 */
public class StandalonePojoBootstrapIntrospector extends AbstractPojoModelsBootstrapIntrospector
		implements PojoBootstrapIntrospector {

	public static StandalonePojoBootstrapIntrospector create(IndexView indexView, ValueHandleFactory valueHandleFactory) {
		return new StandalonePojoBootstrapIntrospector( indexView, valueHandleFactory );
	}


	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	private StandalonePojoBootstrapIntrospector(IndexView indexView, ValueHandleFactory valueHandleFactory) {
		super( simpleClassDetailsRegistry( indexView ), valueHandleFactory );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoRawTypeModel<T> typeModel(Class<T> clazz) {
		if ( clazz.isPrimitive() ) {
			/*
			 * We'll never manipulate the primitive type, as we're using generics everywhere,
			 * so let's consider every occurrence of the primitive type as an occurrence of its wrapper type.
			 */
			clazz = (Class<T>) ReflectionHelper.getPrimitiveWrapperType( clazz );
		}
		return (PojoSimpleModelsRawTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public PojoRawTypeModel<?> typeModel(String name) {
		throw MappingLog.INSTANCE.namedTypesNotSupported( name );
	}

	@Override
	protected ValueReadHandle<?> createValueReadHandle(Member member) throws IllegalAccessException {
		setAccessible( member );
		return super.createValueReadHandle( member );
	}

	@Override
	protected ValueReadWriteHandle<?> createValueReadWriteHandle(Member member) throws IllegalAccessException {
		setAccessible( member );
		return super.createValueReadWriteHandle( member );
	}

	@Override
	protected <T> ValueCreateHandle<T> createValueCreateHandle(Constructor<T> constructor) throws IllegalAccessException {
		setAccessible( constructor );
		return valueHandleFactory.createForConstructor( constructor );
	}

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> clazz) {
		PojoRawTypeIdentifier<T> typeIdentifier = PojoRawTypeIdentifier.of( clazz );
		try {
			return new PojoSimpleModelsRawTypeModel<>(
					this, typeIdentifier,
					new RawTypeDeclaringContext<>( genericContextHelper, clazz )
			);
		}
		catch (RuntimeException e) {
			throw MappingLog.INSTANCE.errorRetrievingTypeModel( clazz, e );
		}
	}

	private static void setAccessible(Member member) {
		// always try to set accessible to true regardless of visibility
		// as it's faster even for public fields:
		// it bypasses the security model checks at execution time.
		( (AccessibleObject) member ).setAccessible( true );
	}
}
