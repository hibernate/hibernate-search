/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.testsupport.TestIntrospector;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture.Of;

import org.junit.jupiter.api.Test;

import org.assertj.core.api.InstanceOfAssertFactories;

class TypePatternMatcherFactoryTest {

	private final TestIntrospector introspector =
			new TestIntrospector( ValueHandleFactory.usingMethodHandle( MethodHandles.lookup() ) );
	private final TypePatternMatcherFactory factory = new TypePatternMatcherFactory( introspector );

	@Test
	void exactType() {
		TypePatternMatcher matcher = factory.createExactRawTypeMatcher( CharSequence.class );
		assertThat( matcher ).isNotNull();

		assertThat( matcher )
				.hasToString( "hasExactRawType(java.lang.CharSequence)" );

		// Exact type => match
		assertThat( matcher.matches( introspector.typeModel( CharSequence.class ) ) ).isTrue();

		// Supertype => no match
		assertThat( matcher.matches( introspector.typeModel( Object.class ) ) ).isFalse();

		// Subtype => no match
		assertThat( matcher.matches( introspector.typeModel( String.class ) ) ).isFalse();

		// Unrelated type => no match
		assertThat( matcher.matches( introspector.typeModel( Number.class ) ) ).isFalse();
	}

	/**
	 * Simulate a pattern matching concrete enum types, but not the Enum class.
	 * Useful for bridge mapping in particular.
	 */
	@Test
	void concreteEnumType() {
		TypePatternMatcher matcher = factory.createRawSuperTypeMatcher( Enum.class )
				.and( factory.createExactRawTypeMatcher( Enum.class ).negate() );
		assertThat( matcher ).isNotNull();

		// Strict Enum subtype => match
		assertThat( matcher.matches( introspector.typeModel( MyEnum.class ) ) ).isTrue();

		// Enum class itself => no match
		assertThat( matcher.matches( introspector.typeModel( Enum.class ) ) ).isFalse();

		// Enum supertype => no match
		assertThat( matcher.matches( introspector.typeModel( Object.class ) ) ).isFalse();

		// Unrelated type => no match
		assertThat( matcher.matches( introspector.typeModel( Number.class ) ) ).isFalse();
	}

	private enum MyEnum {
		FOO, BAR;
	}

	@Test
	void wildcardType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new WildcardTypeCapture<Of<?>>() {}.getType(),
				String.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T> void typeVariable() {
		// Must put this here, not in the lambda, otherwise the generated type is a bit different.
		Type type = new TypeCapture<T>() {}.getType();
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				type,
				String.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void rawSuperType() {
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher( Collection.class, Integer.class );
		assertThat( matcher ).isNotNull();

		assertThat( matcher )
				.hasToString( "hasRawSuperType(java.util.Collection) => java.lang.Integer" );

		assertThat( matcher.extract( introspector.typeModel( EnumSet.class ) ) )
				.asInstanceOf( InstanceOfAssertFactories.OPTIONAL )
				.contains( introspector.typeModel( Integer.class ) );

		assertThat( matcher.extract( introspector.typeModel( Iterable.class ) ) )
				.isEmpty();
	}

	@Test
	<T> void rawSuperType_resultIsTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				String.class,
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void rawSuperType_resultIsWildcard() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				String.class,
				new WildcardTypeCapture<Of<?>>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void rawSuperType_resultIsParameterized() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				String.class,
				new TypeCapture<List<String>>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void nonGenericArrayElement() {
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher( String[].class, Integer.class );
		assertThat( matcher ).isNotNull();

		assertThat( matcher )
				.hasToString( "hasRawSuperType([Ljava.lang.String;) => java.lang.Integer" );

		assertThat( matcher.extract( introspector.typeModel( String[].class ) ) )
				.asInstanceOf( InstanceOfAssertFactories.OPTIONAL )
				.contains( introspector.typeModel( Integer.class ) );
	}

	@Test
	<T, U> void genericArrayElement() {
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher(
				new TypeCapture<T[]>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		);
		assertThat( matcher ).isInstanceOf( ArrayElementTypeMatcher.class );
		assertThat( matcher )
				.hasToString( "T[] => T" );

		assertThat( matcher.extract( introspector.typeModel( new TypeCapture<U[]>() {} ) ) )
				.asInstanceOf( InstanceOfAssertFactories.OPTIONAL )
				.contains( introspector.typeModel( new TypeCapture<U>() {} ) );

		assertThat( matcher.extract( introspector.typeModel( new TypeCapture<U>() {} ) ) )
				.isEmpty();
	}

	@Test
	<T extends Iterable<?>> void genericArrayElement_boundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T extends Object & Serializable> void genericArrayElement_multiBoundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T> void genericArrayElement_resultIsRawType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() {}.getType(),
				Object.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T, U> void genericArrayElement_resultIsDifferentTypeArgument() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() {}.getType(),
				new TypeCapture<U>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T, U> void parameterizedType() {
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		);
		assertThat( matcher ).isInstanceOf( ParameterizedTypeArgumentMatcher.class );
		assertThat( matcher )
				.hasToString( "java.util.Map<?, T> => T" );

		assertThat( matcher.extract( introspector.typeModel( new TypeCapture<Map<String, U>>() {} ) ) )
				.asInstanceOf( InstanceOfAssertFactories.OPTIONAL )
				.contains( introspector.typeModel( new TypeCapture<U>() {} ) );

		assertThat( matcher.extract( introspector.typeModel( new TypeCapture<Collection<U>>() {} ) ) )
				.isEmpty();
		assertThat( matcher.extract( introspector.typeModel( new TypeCapture<U>() {} ) ) )
				.isEmpty();
	}

	@Test
	<T> void parameterizedType_upperBoundedWildcard() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<? extends Long, T>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T> void parameterizedType_lowerBoundedWildcard() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<? super Long, T>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T> void parameterizedType_onlyWildcards() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, ?>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T> void parameterizedType_rawType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, String>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T extends Iterable<?>> void parameterizedType_boundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T extends Object & Serializable> void parameterizedType_multiBoundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T, U> void parameterizedType_multipleTypeVariables() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<T, U>>() {}.getType(),
				new TypeCapture<T>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T> void parameterizedType_resultIsRawType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() {}.getType(),
				Object.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	<T, U> void parameterizedType_resultIsDifferentTypeArgument() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() {}.getType(),
				new TypeCapture<U>() {}.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

}
