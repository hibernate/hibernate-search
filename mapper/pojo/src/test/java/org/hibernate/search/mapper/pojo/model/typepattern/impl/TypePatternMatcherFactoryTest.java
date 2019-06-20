/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture.Of;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class TypePatternMatcherFactoryTest extends EasyMockSupport {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

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
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new WildcardTypeCapture<Of<?>>() { }.getType(),
				String.class
		);
	}

	@Test
	public <T> void typeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<T>() { }.getType(),
				String.class
		);
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
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				String.class,
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void rawSuperType_resultIsWildcard() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				String.class,
				new WildcardTypeCapture<Of<?>>() { }.getType()
		);
	}

	@Test
	public void rawSuperType_resultIsParameterized() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				String.class,
				new TypeCapture<List<String>>() { }.getType()
		);
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
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T extends Object & Serializable> void genericArrayElement_multiBoundedTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void genericArrayElement_resultIsRawType() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				Object.class
		);
	}

	@Test
	public <T, U> void genericArrayElement_resultIsDifferentTypeArgument() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<U>() { }.getType()
		);
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
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<? extends Long, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void parameterizedType_lowerBoundedWildcard() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<? super Long, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void parameterizedType_onlyWildcards() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<?, ?>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void parameterizedType_rawType() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<?, String>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T extends Iterable<?>> void parameterizedType_boundedTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T extends Object & Serializable> void parameterizedType_multiBoundedTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T, U> void parameterizedType_multipleTypeVariables() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<T, U>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T, U> void parameterizedType_resultIsRawType() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				Object.class
		);
	}

	@Test
	public <T, U> void parameterizedType_resultIsDifferentTypeArgument() {
		thrown.expect( UnsupportedOperationException.class );

		factory.createExtractingMatcher(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<U>() { }.getType()
		);
	}

}
