/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.model.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoHCannOrmGenericContextHelper;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoSimpleHCAnnRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * A very simple introspector for Pojo mapping in standalone mode (without Hibernate ORM).
 */
public class StandalonePojoBootstrapIntrospector extends AbstractPojoHCAnnBootstrapIntrospector
		implements PojoBootstrapIntrospector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static StandalonePojoBootstrapIntrospector create(ValueHandleFactory valueHandleFactory) {
		return new StandalonePojoBootstrapIntrospector( valueHandleFactory );
	}

	private final PojoHCannOrmGenericContextHelper genericContextHelper;

	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	private StandalonePojoBootstrapIntrospector(ValueHandleFactory valueHandleFactory) {
		super( new JavaReflectionManager(), valueHandleFactory );
		this.genericContextHelper = new PojoHCannOrmGenericContextHelper( this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoSimpleHCAnnRawTypeModel<T> typeModel(Class<T> clazz) {
		if ( clazz.isPrimitive() ) {
			/*
			 * We'll never manipulate the primitive type, as we're using generics everywhere,
			 * so let's consider every occurrence of the primitive type as an occurrence of its wrapper type.
			 */
			clazz = (Class<T>) ReflectionHelper.getPrimitiveWrapperType( clazz );
		}
		return (PojoSimpleHCAnnRawTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public PojoRawTypeModel<?> typeModel(String name) {
		throw log.namedTypesNotSupported( name );
	}

	@Override
	protected ValueReadHandle<?> createValueReadHandle(Member member) throws IllegalAccessException {
		setAccessible( member );
		return super.createValueReadHandle( member );
	}

	@Override
	protected <T> ValueCreateHandle<T> createValueCreateHandle(Constructor<T> constructor) throws IllegalAccessException {
		setAccessible( constructor );
		return valueHandleFactory.createForConstructor( constructor );
	}

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> clazz) {
		PojoRawTypeIdentifier<T> typeIdentifier = PojoRawTypeIdentifier.of( clazz );
		try {
			return new PojoSimpleHCAnnRawTypeModel<>(
					this, typeIdentifier,
					new RawTypeDeclaringContext<>( genericContextHelper, clazz )
			);
		}
		catch (RuntimeException e) {
			throw log.errorRetrievingTypeModel( clazz, e );
		}
	}

	private static void setAccessible(Member member) {
		try {
			// always try to set accessible to true regardless of visibility
			// as it's faster even for public fields:
			// it bypasses the security model checks at execution time.
			( (AccessibleObject) member ).setAccessible( true );
		}
		catch (SecurityException se) {
			if ( !Modifier.isPublic( member.getModifiers() ) ) {
				throw se;
			}
		}
	}
}
