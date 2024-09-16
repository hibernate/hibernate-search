/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.models.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.models.internal.BasicModelBuildingContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.jandex.internal.JandexModelBuildingContextImpl;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.StreamHelper;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

import org.jboss.jandex.IndexView;

public abstract class AbstractPojoModelsBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final PojoModelsClassOrdering typeOrdering;
	protected final ValueHandleFactory valueHandleFactory;
	private final ClassDetailsRegistry classDetailsRegistry;

	public AbstractPojoModelsBootstrapIntrospector(ValueHandleFactory valueHandleFactory) {
		this( simpleClassDetailsRegistry( null ), valueHandleFactory );
	}

	public AbstractPojoModelsBootstrapIntrospector(ClassDetailsRegistry classDetailsRegistry,
			ValueHandleFactory valueHandleFactory) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.typeOrdering = new PojoModelsClassOrdering( classDetailsRegistry );
		this.valueHandleFactory = valueHandleFactory;
	}

	protected static ClassDetailsRegistry simpleClassDetailsRegistry(IndexView indexView) {
		if ( indexView == null ) {
			return new BasicModelBuildingContextImpl(
					SimpleClassLoading.SIMPLE_CLASS_LOADING
			).getClassDetailsRegistry();
		}
		else {
			return new JandexModelBuildingContextImpl(
					indexView,
					SimpleClassLoading.SIMPLE_CLASS_LOADING,
					null
			).getClassDetailsRegistry();
		}
	}

	@Override
	public ValueHandleFactory annotationValueHandleFactory() {
		return valueHandleFactory;
	}

	public Stream<? extends Annotation> annotations(AnnotationTarget annotationTarget) {
		return annotationTarget.getDirectAnnotationUsages().stream();
	}

	public ClassDetails toModelsClass(Class<?> type) {
		return classDetailsRegistry.resolveClassDetails( type.getName() );
	}

	public Map<String, MemberDetails> declaredFieldAccessPropertiesByName(ClassDetails classDetails) {
		return classDetails.getFields().stream()
				.filter( Predicate.not( MemberDetails::isStatic ) )
				.collect( propertiesByNameNoDuplicate() );
	}

	public Map<String, List<MemberDetails>> declaredMethodAccessPropertiesByName(ClassDetails classDetails) {
		return classDetails.getMethods().stream()
				.filter( methodDetails -> methodDetails.getMethodKind().equals( MethodDetails.MethodKind.GETTER )
						&& !methodDetails.isSynthetic() )
				.collect( propertiesByName() );
	}

	public Stream<Class<?>> ascendingSuperClasses(ClassDetails classDetails) {
		return typeOrdering.ascendingSuperTypes( classDetails ).map( this::toClass );
	}

	public Stream<Class<?>> descendingSuperClasses(ClassDetails classDetails) {
		return typeOrdering.descendingSuperTypes( classDetails ).map( this::toClass );
	}

	protected <T> ValueCreateHandle<T> createValueCreateHandle(Constructor<T> constructor)
			throws IllegalAccessException {
		throw new AssertionFailure( this + " doesn't support constructor handles."
				+ " '" + getClass().getName() + " should be updated to implement createValueCreateHandle(Constructor)." );
	}

	protected ValueReadHandle<?> createValueReadHandle(Member member) throws IllegalAccessException {
		if ( member instanceof Method ) {
			Method method = (Method) member;
			return valueHandleFactory.createForMethod( method );
		}
		else if ( member instanceof Field ) {
			Field field = (Field) member;
			return valueHandleFactory.createForField( field );
		}
		else {
			throw new AssertionFailure( "Unexpected type for a " + Member.class.getName() + ": " + member );
		}
	}

	public Class<?> toClass(ClassDetails xClass) {
		return xClass.toJavaClass();
	}

	private static Collector<MemberDetails, ?, Map<String, MemberDetails>> propertiesByNameNoDuplicate() {
		return StreamHelper.toMap(
				MemberDetails::getName, Function.identity(),
				TreeMap::new // Sort properties by name for deterministic iteration
		);
	}

	private static Collector<MemberDetails, ?, Map<String, List<MemberDetails>>> propertiesByName() {
		return Collectors.groupingBy( AbstractPojoModelsBootstrapIntrospector::noPrefix,
				TreeMap::new, // Sort properties by name for deterministic iteration
				Collectors.toList() );
	}

	private static String noPrefix(MemberDetails details) {
		String fullName = details.getName();
		if ( fullName.startsWith( "get" ) ) {
			return decapitalize( fullName.substring( "get".length() ) );
		}
		if ( fullName.startsWith( "is" ) ) {
			return decapitalize( fullName.substring( "is".length() ) );
		}
		return fullName;
	}

	// See conventions expressed by https://docs.oracle.com/javase/7/docs/api/java/beans/Introspector.html#decapitalize(java.lang.String)
	private static String decapitalize(String name) {
		if ( name != null && !name.isEmpty() ) {
			if ( name.length() > 1 && Character.isUpperCase( name.charAt( 1 ) ) ) {
				return name;
			}
			else {
				char[] chars = name.toCharArray();
				chars[0] = Character.toLowerCase( chars[0] );
				return new String( chars );
			}
		}
		else {
			return name;
		}
	}
}
