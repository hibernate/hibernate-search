/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture.Of;

import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class TypePatternMatcherFactoryTest extends EasyMockSupport {

	private final PojoBootstrapIntrospector introspectorMock = createMock( PojoBootstrapIntrospector.class );

	private final TypePatternMatcherFactory factory = new TypePatternMatcherFactory( introspectorMock );

	@Test
	public void exactType() {
		PojoRawTypeModel typeToMatchMock = createMock( PojoRawTypeModel.class );
		PojoGenericTypeModel typeToInspectMock = createMock( PojoGenericTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = createMock( PojoRawTypeModel.class );

		EasyMock.expect( introspectorMock.getTypeModel( String.class ) )
				.andReturn( typeToMatchMock );
		replayAll();
		TypePatternMatcher matcher = factory.createExactRawTypeMatcher( String.class );
		assertThat( matcher ).isNotNull();
		verifyAll();

		resetAll();
		EasyMock.expect( typeToMatchMock.getName() )
				.andStubReturn( "THE_TYPE_TO_MATCH" );
		replayAll();
		assertThat( matcher.toString() )
				.isEqualTo( "hasExactRawType(THE_TYPE_TO_MATCH)" );
		verifyAll();

		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( true );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.andStubReturn( true );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isTrue();
		verifyAll();

		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( false );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.andStubReturn( true );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
		verifyAll();

		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( true );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.andStubReturn( false );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
		verifyAll();

		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( false );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.andStubReturn( false );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
		verifyAll();
	}

	/**
	 * Simulate a pattern matching concrete enum types, but not the Enum class.
	 * Useful for bridge mapping in particular.
	 */
	@Test
	public void concreteEnumType() {
		PojoRawTypeModel enumTypeMock = createMock( PojoRawTypeModel.class );
		PojoGenericTypeModel typeToInspectMock = createMock( PojoGenericTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = createMock( PojoRawTypeModel.class );

		EasyMock.expect( introspectorMock.getTypeModel( Enum.class ) )
				.andReturn( enumTypeMock ).atLeastOnce();
		replayAll();
		TypePatternMatcher matcher = factory.createRawSuperTypeMatcher( Enum.class )
				.and( factory.createExactRawTypeMatcher( Enum.class ).negate() );
		assertThat( matcher ).isNotNull();
		verifyAll();

		// Strict Enum subtype => match
		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( false );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.andStubReturn( true );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isTrue();
		verifyAll();

		// Enum class itself => no match
		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( true );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.andStubReturn( true );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
		verifyAll();

		// Enum supertype => no match
		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( true );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.andStubReturn( false );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
		verifyAll();

		// Unrelated type => no match
		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock ).atLeastOnce();
		EasyMock.expect( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.andStubReturn( false );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.andStubReturn( false );
		replayAll();
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
		verifyAll();
	}

	@Test
	public void wildcardType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new WildcardTypeCapture<Of<?>>() { }.getType(),
				String.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T> void typeVariable() {
		// Must put this here, not in the lambda, otherwise the generated type is a bit different.
		Type type = new TypeCapture<T>() { }.getType();
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				type,
				String.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public void rawSuperType() {
		PojoRawTypeModel<String> typeToMatchMock = createMock( PojoRawTypeModel.class );
		PojoGenericTypeModel<Integer> resultTypeMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<?> typeToInspectMock = createMock( PojoGenericTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = createMock( PojoRawTypeModel.class );

		EasyMock.expect( introspectorMock.getTypeModel( String.class ) )
				.andReturn( typeToMatchMock );
		EasyMock.expect( introspectorMock.getGenericTypeModel( Integer.class ) )
				.andReturn( resultTypeMock );
		replayAll();
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher( String.class, Integer.class );
		assertThat( matcher ).isNotNull();
		verifyAll();

		resetAll();
		EasyMock.expect( typeToMatchMock.getName() )
				.andReturn( "THE_TYPE_TO_MATCH" );
		EasyMock.expect( resultTypeMock.getName() )
				.andReturn( "THE_RESULT_TYPE" );
		replayAll();
		assertThat( matcher.toString() )
				.isEqualTo( "hasRawSuperType(THE_TYPE_TO_MATCH) => THE_RESULT_TYPE" );
		verifyAll();

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.andReturn( true );
		replayAll();
		actualReturn = matcher.extract( typeToInspectMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( (PojoRawTypeModel) typeToInspectRawTypeMock );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.andReturn( false );
		replayAll();
		actualReturn = matcher.extract( typeToInspectMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isFalse();
	}

	@Test
	public <T> void rawSuperType_resultIsTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				String.class,
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T> void rawSuperType_resultIsWildcard() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				String.class,
				new WildcardTypeCapture<Of<?>>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public void rawSuperType_resultIsParameterized() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				String.class,
				new TypeCapture<List<String>>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public void nonGenericArrayElement() {
		PojoRawTypeModel<String[]> typeToMatchMock = createMock( PojoRawTypeModel.class );
		PojoGenericTypeModel<Integer> resultTypeMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<?> typeToInspectMock = createMock( PojoGenericTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = createMock( PojoRawTypeModel.class );

		EasyMock.expect( introspectorMock.getTypeModel( String[].class ) )
				.andReturn( typeToMatchMock );
		EasyMock.expect( introspectorMock.getGenericTypeModel( Integer.class ) )
				.andReturn( resultTypeMock );
		replayAll();
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher( String[].class, Integer.class );
		assertThat( matcher ).isNotNull();
		verifyAll();

		resetAll();
		EasyMock.expect( typeToMatchMock.getName() )
				.andReturn( "THE_TYPE_TO_MATCH" );
		EasyMock.expect( resultTypeMock.getName() )
				.andReturn( "THE_RESULT_TYPE" );
		replayAll();
		assertThat( matcher.toString() )
				.isEqualTo( "hasRawSuperType(THE_TYPE_TO_MATCH) => THE_RESULT_TYPE" );
		verifyAll();

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToInspectMock.getRawType() )
				.andReturn( typeToInspectRawTypeMock );
		EasyMock.expect( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.andReturn( true );
		replayAll();
		actualReturn = matcher.extract( typeToInspectMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );
	}

	@Test
	public <T> void genericArrayElement() {
		PojoGenericTypeModel<?> typeToInspectMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<T> resultTypeMock = createMock( PojoGenericTypeModel.class );

		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
		assertThat( matcher ).isInstanceOf( ArrayElementTypeMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "T[] => T" );

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToInspectMock.getArrayElementType() )
				.andReturn( (Optional) Optional.of( resultTypeMock ) );
		replayAll();
		actualReturn = matcher.extract( typeToInspectMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		resetAll();
		EasyMock.expect( typeToInspectMock.getArrayElementType() )
				.andReturn( Optional.empty() );
		replayAll();
		actualReturn = matcher.extract( typeToInspectMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isFalse();
	}

	@Test
	public <T extends Iterable<?>> void genericArrayElement_boundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T extends Object & Serializable> void genericArrayElement_multiBoundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T> void genericArrayElement_resultIsRawType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				Object.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T, U> void genericArrayElement_resultIsDifferentTypeArgument() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<U>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T> void parameterizedType() {
		PojoGenericTypeModel<?> typeToInspectMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<Integer> resultTypeMock = createMock( PojoGenericTypeModel.class );

		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
		assertThat( matcher ).isInstanceOf( ParameterizedTypeArgumentMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "java.util.Map<?, T> => T" );

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToInspectMock.getTypeArgument( Map.class, 1 ) )
				.andReturn( (Optional) Optional.of( resultTypeMock ) );
		replayAll();
		actualReturn = matcher.extract( typeToInspectMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		resetAll();
		EasyMock.expect( typeToInspectMock.getTypeArgument( Map.class, 1 ) )
				.andReturn( Optional.empty() );
		replayAll();
		actualReturn = matcher.extract( typeToInspectMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isFalse();
	}

	@Test
	public <T> void parameterizedType_upperBoundedWildcard() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<? extends Long, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T> void parameterizedType_lowerBoundedWildcard() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<? super Long, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T> void parameterizedType_onlyWildcards() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, ?>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T> void parameterizedType_rawType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, String>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T extends Iterable<?>> void parameterizedType_boundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T extends Object & Serializable> void parameterizedType_multiBoundedTypeVariable() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T, U> void parameterizedType_multipleTypeVariables() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<T, U>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T, U> void parameterizedType_resultIsRawType() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				Object.class
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	public <T, U> void parameterizedType_resultIsDifferentTypeArgument() {
		assertThatThrownBy( () -> factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<U>() { }.getType()
		) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

}
