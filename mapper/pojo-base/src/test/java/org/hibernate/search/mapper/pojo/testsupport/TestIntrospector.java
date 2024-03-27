/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.testsupport;

import java.lang.reflect.Type;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoHCannOrmGenericContextHelper;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoSimpleHCAnnRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;

public class TestIntrospector extends AbstractPojoHCAnnBootstrapIntrospector {
	private final PojoHCannOrmGenericContextHelper genericContextHelper = new PojoHCannOrmGenericContextHelper( this );

	public TestIntrospector(ValueHandleFactory valueHandleFactory) {
		super( new JavaReflectionManager(), valueHandleFactory );
	}

	@Override
	public <T> PojoRawTypeModel<T> typeModel(Class<T> clazz) {
		return new PojoSimpleHCAnnRawTypeModel<>( this, PojoRawTypeIdentifier.of( clazz ),
				new RawTypeDeclaringContext<>( genericContextHelper, clazz ) );
	}

	@Override
	public PojoRawTypeModel<?> typeModel(String name) {
		throw new AssertionFailure( "This method is not supported" );
	}

	@SuppressWarnings("unchecked")
	public <T> PojoTypeModel<T> typeModel(TypeCapture<T> typeCapture) {
		Type type = typeCapture.getType();
		if ( type instanceof Class ) {
			return (PojoTypeModel<T>) typeModel( (Class<?>) type );
		}
		else {
			RawTypeDeclaringContext<?> rootContext = new RawTypeDeclaringContext<>( genericContextHelper, TypeCapture.class );
			PojoTypeModel<?> typeCaptureType = rootContext.memberTypeReference( typeCapture.getClass() );
			return (PojoTypeModel<T>) typeCaptureType.typeArgument( TypeCapture.class, 0 )
					.orElseThrow( () -> new IllegalArgumentException(
							typeCapture.getClass() + " doesn't extend or implement " + TypeCapture.class
									+ " directly with a type argument" ) );
		}
	}
}
