/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.models.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.AbstractPojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public abstract class AbstractPojoModelsRawTypeModel<
		T,
		I extends AbstractPojoModelsBootstrapIntrospector,
		P extends PojoPropertyModel<?>>
		extends AbstractPojoRawTypeModel<T, I, P> {

	protected final ClassDetails classDetails;
	final RawTypeDeclaringContext<T> rawTypeDeclaringContext;

	private Map<String, MemberDetails> declaredFieldAccessPropertiesByName;
	private Map<String, List<MemberDetails>> declaredMethodAccessPropertiesByName;

	public AbstractPojoModelsRawTypeModel(I introspector, PojoRawTypeIdentifier<T> typeIdentifier,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, typeIdentifier );
		this.classDetails = introspector.toModelsClass( typeIdentifier.javaClass() );
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
	}

	@Override
	public boolean isAbstract() {
		return classDetails.isAbstract();
	}

	@Override
	public final boolean isSubTypeOf(MappableTypeModel other) {
		return other instanceof AbstractPojoModelsRawTypeModel
				&& ( (AbstractPojoModelsRawTypeModel<?, ?, ?>) other ).classDetails.toJavaClass()
						.isAssignableFrom( classDetails.toJavaClass() );
	}

	@Override
	public Optional<PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		return rawTypeDeclaringContext.typeArgument( rawSuperType, typeParameterIndex );
	}

	@Override
	public Optional<PojoTypeModel<?>> arrayElementType() {
		return rawTypeDeclaringContext.arrayElementType();
	}

	@Override
	public Stream<? extends Annotation> annotations() {
		return introspector.annotations( classDetails );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<PojoConstructorModel<T>> createDeclaredConstructors() {
		return Arrays.stream( javaClass().getDeclaredConstructors() )
				.<PojoConstructorModel<T>>map( constructor -> new PojoModelsConstructorModel<>(
						introspector, this, (Constructor<T>) constructor ) )
				.collect( Collectors.toList() );
	}

	Class<T> javaClass() {
		return typeIdentifier.javaClass();
	}

	@Override
	protected Stream<String> declaredPropertyNames() {
		return Stream.concat(
				declaredFieldAccessPropertiesByName().keySet().stream(),
				declaredMethodAccessPropertiesByName().keySet().stream()
		).distinct();
	}

	protected final Map<String, MemberDetails> declaredFieldAccessPropertiesByName() {
		if ( declaredFieldAccessPropertiesByName == null ) {
			declaredFieldAccessPropertiesByName =
					introspector.declaredFieldAccessPropertiesByName( classDetails );
		}
		return declaredFieldAccessPropertiesByName;
	}

	protected final Map<String, List<MemberDetails>> declaredMethodAccessPropertiesByName() {
		if ( declaredMethodAccessPropertiesByName == null ) {
			declaredMethodAccessPropertiesByName =
					introspector.declaredMethodAccessPropertiesByName( classDetails );
		}
		return declaredMethodAccessPropertiesByName;
	}

	protected final List<Member> declaredPropertyGetters(String propertyName) {
		List<MemberDetails> methodAccessProperties = declaredMethodAccessPropertiesByName().get( propertyName );
		if ( methodAccessProperties != null ) {
			return methodAccessProperties.stream().map( MemberDetails::toJavaMember )
					.collect( Collectors.toList() );
		}
		return null;
	}

	protected final Member declaredPropertyField(String propertyName) {
		MemberDetails fieldAccessProperty = classDetails.findFieldByName( propertyName );
		if ( fieldAccessProperty != null ) {
			return fieldAccessProperty.toJavaMember();
		}
		return null;
	}

}
