/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture.Of;

import org.junit.Test;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types
public class TypePatternMatcherFactoryTest {

	private final PojoBootstrapIntrospector introspectorMock = mock( PojoBootstrapIntrospector.class );

	private final TypePatternMatcherFactory factory = new TypePatternMatcherFactory( introspectorMock );

	@Test
	public void exactType() {
		PojoRawTypeModel typeToMatchMock = mock( PojoRawTypeModel.class );
		PojoTypeModel typeToInspectMock = mock( PojoTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = mock( PojoRawTypeModel.class );

		when( introspectorMock.typeModel( String.class ) )
				.thenReturn( typeToMatchMock );
		TypePatternMatcher matcher = factory.createExactRawTypeMatcher( String.class );
		assertThat( matcher ).isNotNull();

		when( typeToMatchMock.name() )
				.thenReturn( "THE_TYPE_TO_MATCH" );
		assertThat( matcher.toString() )
				.isEqualTo( "hasExactRawType(THE_TYPE_TO_MATCH)" );

		when( typeToInspectMock.rawType() )
				.thenReturn( typeToInspectRawTypeMock );
		when( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( true );
		when( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.thenReturn( true );
		assertThat( matcher.matches( typeToInspectMock ) ).isTrue();

		when( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( false );
		when( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.thenReturn( true );
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();

		when( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( true );
		when( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.thenReturn( false );
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();

		when( typeToMatchMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( false );
		when( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.thenReturn( false );
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
	}

	/**
	 * Simulate a pattern matching concrete enum types, but not the Enum class.
	 * Useful for bridge mapping in particular.
	 */
	@Test
	public void concreteEnumType() {
		PojoRawTypeModel enumTypeMock = mock( PojoRawTypeModel.class );
		PojoTypeModel typeToInspectMock = mock( PojoTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = mock( PojoRawTypeModel.class );

		when( introspectorMock.typeModel( Enum.class ) )
				.thenReturn( enumTypeMock );
		TypePatternMatcher matcher = factory.createRawSuperTypeMatcher( Enum.class )
				.and( factory.createExactRawTypeMatcher( Enum.class ).negate() );
		assertThat( matcher ).isNotNull();

		// Strict Enum subtype => match
		when( typeToInspectMock.rawType() )
				.thenReturn( typeToInspectRawTypeMock );
		when( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( false );
		when( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.thenReturn( true );
		assertThat( matcher.matches( typeToInspectMock ) ).isTrue();

		// Enum class itself => no match
		when( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( true );
		when( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.thenReturn( true );
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();

		// Enum supertype => no match
		when( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( true );
		when( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.thenReturn( false );
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();

		// Unrelated type => no match
		when( enumTypeMock.isSubTypeOf( typeToInspectRawTypeMock ) )
				.thenReturn( false );
		when( typeToInspectRawTypeMock.isSubTypeOf( enumTypeMock ) )
				.thenReturn( false );
		assertThat( matcher.matches( typeToInspectMock ) ).isFalse();
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
		PojoRawTypeModel<String> typeToMatchMock = mock( PojoRawTypeModel.class );
		PojoRawTypeModel<Integer> resultTypeMock = mock( PojoRawTypeModel.class );
		PojoTypeModel<?> typeToInspectMock = mock( PojoTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = mock( PojoRawTypeModel.class );

		when( introspectorMock.typeModel( String.class ) )
				.thenReturn( typeToMatchMock );
		when( introspectorMock.typeModel( Integer.class ) )
				.thenReturn( resultTypeMock );
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher( String.class, Integer.class );
		assertThat( matcher ).isNotNull();

		when( typeToMatchMock.name() )
				.thenReturn( "THE_TYPE_TO_MATCH" );
		when( resultTypeMock.name() )
				.thenReturn( "THE_RESULT_TYPE" );
		assertThat( matcher.toString() )
				.isEqualTo( "hasRawSuperType(THE_TYPE_TO_MATCH) => THE_RESULT_TYPE" );

		Optional<? extends PojoTypeModel<?>> actualReturn;

		when( typeToInspectMock.rawType() )
				.thenReturn( typeToInspectRawTypeMock );
		when( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.thenReturn( true );
		actualReturn = matcher.extract( typeToInspectMock );
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		when( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.thenReturn( false );
		actualReturn = matcher.extract( typeToInspectMock );
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
		PojoRawTypeModel<String[]> typeToMatchMock = mock( PojoRawTypeModel.class );
		PojoRawTypeModel<Integer> resultTypeMock = mock( PojoRawTypeModel.class );
		PojoTypeModel<?> typeToInspectMock = mock( PojoTypeModel.class );
		PojoRawTypeModel typeToInspectRawTypeMock = mock( PojoRawTypeModel.class );

		when( introspectorMock.typeModel( String[].class ) )
				.thenReturn( typeToMatchMock );
		when( introspectorMock.typeModel( Integer.class ) )
				.thenReturn( resultTypeMock );
		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher( String[].class, Integer.class );
		assertThat( matcher ).isNotNull();

		when( typeToMatchMock.name() )
				.thenReturn( "THE_TYPE_TO_MATCH" );
		when( resultTypeMock.name() )
				.thenReturn( "THE_RESULT_TYPE" );
		assertThat( matcher.toString() )
				.isEqualTo( "hasRawSuperType(THE_TYPE_TO_MATCH) => THE_RESULT_TYPE" );

		Optional<? extends PojoTypeModel<?>> actualReturn;

		when( typeToInspectMock.rawType() )
				.thenReturn( typeToInspectRawTypeMock );
		when( typeToInspectRawTypeMock.isSubTypeOf( typeToMatchMock ) )
				.thenReturn( true );
		actualReturn = matcher.extract( typeToInspectMock );
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );
	}

	@Test
	public <T> void genericArrayElement() {
		PojoTypeModel<?> typeToInspectMock = mock( PojoTypeModel.class );
		PojoTypeModel<T> resultTypeMock = mock( PojoTypeModel.class );

		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
		assertThat( matcher ).isInstanceOf( ArrayElementTypeMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "T[] => T" );

		Optional<? extends PojoTypeModel<?>> actualReturn;

		when( typeToInspectMock.arrayElementType() )
				.thenReturn( (Optional) Optional.of( resultTypeMock ) );
		actualReturn = matcher.extract( typeToInspectMock );
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		when( typeToInspectMock.arrayElementType() )
				.thenReturn( Optional.empty() );
		actualReturn = matcher.extract( typeToInspectMock );
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
		PojoTypeModel<?> typeToInspectMock = mock( PojoTypeModel.class );
		PojoTypeModel<Integer> resultTypeMock = mock( PojoTypeModel.class );

		ExtractingTypePatternMatcher matcher = factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
		assertThat( matcher ).isInstanceOf( ParameterizedTypeArgumentMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "java.util.Map<?, T> => T" );

		Optional<? extends PojoTypeModel<?>> actualReturn;

		when( typeToInspectMock.typeArgument( Map.class, 1 ) )
				.thenReturn( (Optional) Optional.of( resultTypeMock ) );
		actualReturn = matcher.extract( typeToInspectMock );
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		when( typeToInspectMock.typeArgument( Map.class, 1 ) )
				.thenReturn( Optional.empty() );
		actualReturn = matcher.extract( typeToInspectMock );
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
