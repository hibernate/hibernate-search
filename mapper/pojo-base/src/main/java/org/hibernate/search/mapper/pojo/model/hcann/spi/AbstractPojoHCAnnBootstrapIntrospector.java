/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.StreamHelper;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public abstract class AbstractPojoHCAnnBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final ReflectionManager reflectionManager;
	private final PojoXClassOrdering typeOrdering;
	protected final ValueHandleFactory valueHandleFactory;

	public AbstractPojoHCAnnBootstrapIntrospector(ReflectionManager reflectionManager,
			ValueHandleFactory valueHandleFactory) {
		this.reflectionManager = reflectionManager;
		this.typeOrdering = new PojoXClassOrdering( reflectionManager );
		this.valueHandleFactory = valueHandleFactory;
	}

	@Override
	public ValueHandleFactory annotationValueHandleFactory() {
		return valueHandleFactory;
	}

	public Stream<Annotation> annotations(XAnnotatedElement xAnnotated) {
		return Arrays.stream( xAnnotated.getAnnotations() );
	}

	public XClass toXClass(Class<?> type) {
		return reflectionManager.toXClass( type );
	}

	public Map<String, XProperty> declaredFieldAccessXPropertiesByName(XClass xClass) {
		return xClass.getDeclaredProperties( XClass.ACCESS_FIELD ).stream()
				.collect( xPropertiesByNameNoDuplicate() );
	}

	public Map<String, List<XProperty>> declaredMethodAccessXPropertiesByName(XClass xClass) {
		return xClass.getDeclaredProperties( XClass.ACCESS_PROPERTY ).stream()
				.collect( xPropertiesByName() );
	}

	public Stream<Class<?>> ascendingSuperClasses(XClass xClass) {
		return typeOrdering.ascendingSuperTypes( xClass ).map( this::toClass );
	}

	public Stream<Class<?>> descendingSuperClasses(XClass xClass) {
		return typeOrdering.descendingSuperTypes( xClass ).map( this::toClass );
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

	public Class<?> toClass(XClass xClass) {
		return reflectionManager.toClass( xClass );
	}

	private static Collector<XProperty, ?, Map<String, XProperty>> xPropertiesByNameNoDuplicate() {
		return StreamHelper.toMap(
				XProperty::getName, Function.identity(),
				TreeMap::new // Sort properties by name for deterministic iteration
		);
	}

	private static Collector<XProperty, ?, Map<String, List<XProperty>>> xPropertiesByName() {
		return Collectors.groupingBy( XProperty::getName,
				TreeMap::new, // Sort properties by name for deterministic iteration
				Collectors.toList() );
	}
}
