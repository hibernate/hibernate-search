/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.testsupport;

import java.lang.reflect.Type;

import org.hibernate.search.engine.environment.classpath.spi.AggregatedClassLoader;
import org.hibernate.search.engine.environment.classpath.spi.DefaultClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultResourceResolver;
import org.hibernate.search.mapper.pojo.model.models.spi.AbstractPojoModelsBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.models.spi.PojoModelsGenericContextHelper;
import org.hibernate.search.mapper.pojo.model.models.spi.PojoSimpleModelsRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;

public class TestIntrospector extends AbstractPojoModelsBootstrapIntrospector {
	private final PojoModelsGenericContextHelper genericContextHelper = new PojoModelsGenericContextHelper( this );

	public TestIntrospector(ValueHandleFactory valueHandleFactory) {
		this( valueHandleFactory, AggregatedClassLoader.createDefault() );
	}

	private TestIntrospector(ValueHandleFactory valueHandleFactory, AggregatedClassLoader aggregatedClassLoader) {
		super( DefaultClassResolver.create( aggregatedClassLoader ), DefaultResourceResolver.create( aggregatedClassLoader ),
				null, valueHandleFactory );
	}

	@Override
	public <T> PojoRawTypeModel<T> typeModel(Class<T> clazz) {
		return new PojoSimpleModelsRawTypeModel<>( this, PojoRawTypeIdentifier.of( clazz ),
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
