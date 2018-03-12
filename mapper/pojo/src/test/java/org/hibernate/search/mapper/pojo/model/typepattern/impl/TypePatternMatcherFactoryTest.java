/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.typepattern.impl;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.test.util.TypeCapture;
import org.hibernate.search.mapper.pojo.test.util.WildcardTypeCapture;
import org.hibernate.search.mapper.pojo.test.util.WildcardTypeCapture.Of;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class TypePatternMatcherFactoryTest extends EasyMockSupport {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final PojoBootstrapIntrospector introspectorMock = createMock( PojoBootstrapIntrospector.class );

	private final TypePatternMatcherFactory factory = new TypePatternMatcherFactory();

	@Test
	public void wildcardType() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new WildcardTypeCapture<Of<?>>() { }.getType(),
				String.class
		);
	}

	@Test
	public <T> void typeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<T>() { }.getType(),
				String.class
		);
	}

	@Test
	public void rawSuperType() {
		PojoGenericTypeModel<?> typeToMatchMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<String> superTypeMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<Integer> resultTypeMock = createMock( PojoGenericTypeModel.class );

		TypePatternMatcher matcher = factory.create( String.class, Integer.class );
		assertThat( matcher ).isInstanceOf( RawSuperTypeMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "java.lang.String => java.lang.Integer" );

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToMatchMock.getSuperType( String.class ) )
				.andReturn( Optional.of( superTypeMock ) );
		EasyMock.expect( introspectorMock.getGenericTypeModel( Integer.class ) )
				.andReturn( resultTypeMock );
		replayAll();
		actualReturn = matcher.match( introspectorMock, typeToMatchMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		resetAll();
		EasyMock.expect( typeToMatchMock.getSuperType( String.class ) )
				.andReturn( Optional.empty() );
		replayAll();
		actualReturn = matcher.match( introspectorMock, typeToMatchMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isFalse();
	}

	@Test
	public <T> void rawSuperType_resultIsTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				String.class,
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void rawSuperType_resultIsWildcard() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				String.class,
				new WildcardTypeCapture<Of<?>>() { }.getType()
		);
	}

	@Test
	public void rawSuperType_resultIsParameterized() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				String.class,
				new TypeCapture<List<String>>() { }.getType()
		);
	}

	@Test
	public void nonGenericArrayElement() {
		PojoGenericTypeModel<?> typeToMatchMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<String[]> superTypeMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<Integer> resultTypeMock = createMock( PojoGenericTypeModel.class );

		TypePatternMatcher matcher = factory.create( String[].class, Integer.class );
		assertThat( matcher ).isInstanceOf( RawSuperTypeMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "[Ljava.lang.String; => java.lang.Integer" );

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToMatchMock.getSuperType( String[].class ) )
				.andReturn( Optional.of( superTypeMock ) );
		EasyMock.expect( introspectorMock.getGenericTypeModel( Integer.class ) )
				.andReturn( resultTypeMock );
		replayAll();
		actualReturn = matcher.match( introspectorMock, typeToMatchMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );
	}

	@Test
	public <T> void genericArrayElement() {
		PojoGenericTypeModel<?> typeToMatchMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<T> resultTypeMock = createMock( PojoGenericTypeModel.class );

		TypePatternMatcher matcher = factory.create(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
		assertThat( matcher ).isInstanceOf( ArrayElementTypeMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "T[] => T" );

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToMatchMock.getArrayElementType() )
				.andReturn( (Optional) Optional.of( resultTypeMock ) );
		replayAll();
		actualReturn = matcher.match( introspectorMock, typeToMatchMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		resetAll();
		EasyMock.expect( typeToMatchMock.getArrayElementType() )
				.andReturn( Optional.empty() );
		replayAll();
		actualReturn = matcher.match( introspectorMock, typeToMatchMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isFalse();
	}

	@Test
	public <T extends Iterable<?>> void genericArrayElement_boundedTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T extends Object & Serializable> void genericArrayElement_multiBoundedTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void genericArrayElement_resultIsRawType() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<T[]>() { }.getType(),
				Object.class
		);
	}

	@Test
	public <T, U> void genericArrayElement_resultIsDifferentTypeArgument() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<T[]>() { }.getType(),
				new TypeCapture<U>() { }.getType()
		);
	}

	@Test
	public <T> void parameterizedType() {
		PojoGenericTypeModel<?> typeToMatchMock = createMock( PojoGenericTypeModel.class );
		PojoGenericTypeModel<Integer> resultTypeMock = createMock( PojoGenericTypeModel.class );

		TypePatternMatcher matcher = factory.create(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
		assertThat( matcher ).isInstanceOf( ParameterizedTypeArgumentMatcher.class );
		assertThat( matcher.toString() )
				.isEqualTo( "java.util.Map<?, T> => T" );

		Optional<? extends PojoGenericTypeModel<?>> actualReturn;

		resetAll();
		EasyMock.expect( typeToMatchMock.getTypeArgument( Map.class, 1 ) )
				.andReturn( (Optional) Optional.of( resultTypeMock ) );
		replayAll();
		actualReturn = matcher.match( introspectorMock, typeToMatchMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isTrue();
		assertThat( actualReturn.get() ).isSameAs( resultTypeMock );

		resetAll();
		EasyMock.expect( typeToMatchMock.getTypeArgument( Map.class, 1 ) )
				.andReturn( Optional.empty() );
		replayAll();
		actualReturn = matcher.match( introspectorMock, typeToMatchMock );
		verifyAll();
		assertThat( actualReturn ).isNotNull();
		assertThat( actualReturn.isPresent() ).isFalse();
	}

	@Test
	public <T> void parameterizedType_upperBoundedWildcard() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<? extends Long, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void parameterizedType_lowerBoundedWildcard() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<? super Long, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void parameterizedType_onlyWildcards() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<?, ?>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T> void parameterizedType_rawType() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<?, String>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T extends Iterable<?>> void parameterizedType_boundedTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T extends Object & Serializable> void parameterizedType_multiBoundedTypeVariable() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T, U> void parameterizedType_multipleTypeVariables() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<T, U>>() { }.getType(),
				new TypeCapture<T>() { }.getType()
		);
	}

	@Test
	public <T, U> void parameterizedType_resultIsRawType() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<?, T>>() { }.getType(),
				Object.class
		);
	}

	@Test
	public <T, U> void parameterizedType_resultIsDifferentTypeArgument() {
		thrown.expect( UnsupportedOperationException.class );

		factory.create(
				new TypeCapture<Map<?, T>>() { }.getType(),
				new TypeCapture<U>() { }.getType()
		);
	}

}
