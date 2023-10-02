/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture;

public abstract class GenericTypeContextAssert {
	public static SimpleGenericTypeContextAssert assertThatTypeContext(GenericTypeContext context) {
		return new SimpleGenericTypeContextAssert( context );
	}

	public abstract GenericTypeContext getTypeContext();

	public GenericTypeContextAssert hasRawType(Class<?> expected) {
		assertThat( getTypeContext().rawType() ).isEqualTo( expected );
		return this;
	}

	public GenericTypeContextAssert hasName(String expected) {
		assertThat( getTypeContext().name() ).isEqualTo( expected );
		return this;
	}

	public GenericTypeContextAssert resolveArrayElementTypeTo(Type expected) {
		Optional<Type> optional = getTypeContext().resolveArrayElementType();
		assertThat( optional.isPresent() )
				.as( "Expected " + getTypeContext() + " to be considered an array" )
				.isTrue();
		assertThat( optional.get() )
				.isEqualTo( expected );
		return this;
	}

	public GenericTypeContextAssert resolveArrayElementTypeToEmpty() {
		Optional<Type> optional = getTypeContext().resolveArrayElementType();
		assertThat( optional.isPresent() )
				.as( "Expected " + getTypeContext() + " NOT to be considered an array" )
				.isFalse();
		return this;
	}

	public GenericTypeContextAssert resolveTypeArgumentTo(TypeCapture<?> expected, Class<?> rawSuperClass,
			int typeArgumentIndex) {
		return resolveTypeArgumentTo( expected.getType(), rawSuperClass, typeArgumentIndex );
	}

	public GenericTypeContextAssert resolveTypeArgumentTo(Type expected, Class<?> rawSuperClass,
			int typeArgumentIndex) {
		Optional<Type> optional = getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
		assertThat( optional.isPresent() )
				.as( "Expected " + getTypeContext() + " to be considered a subtype of " + rawSuperClass )
				.isTrue();
		assertThat( optional.get() )
				.isEqualTo( expected );
		return this;
	}

	public GenericTypeContextAssert resolveTypeArgumentToEmpty(Class<?> rawSuperClass, int typeArgumentIndex) {
		Optional<?> optional = getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
		assertThat( optional.isPresent() )
				.as( "Expected " + getTypeContext() + " NOT to be considered a subtype of " + rawSuperClass )
				.isFalse();
		return this;
	}

	public GenericTypeContextAssert noTypeParameter(Class<?> rawSuperClass, int typeArgumentIndex) {
		try {
			getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
			fail( "Expected resolveTypeArgument(" + rawSuperClass + ", " + typeArgumentIndex + ")"
					+ " for type " + getTypeContext() + " to fail because " + rawSuperClass
					+ " doesn't have any type parameter" );
		}
		catch (IllegalArgumentException e) {
			assertThat( e.getMessage() )
					.contains( rawSuperClass.getName() )
					.contains( "doesn't declare any type parameter" );
		}
		return this;
	}

	public GenericTypeContextAssert typeParameterIndexTooLow(Class<?> rawSuperClass, int typeArgumentIndex) {
		try {
			getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
			fail( "Expected resolveTypeArgument(" + rawSuperClass + ", " + typeArgumentIndex + ")"
					+ " for type " + getTypeContext() + " to fail because of the invalid index" );
		}
		catch (IllegalArgumentException e) {
			assertThat( e.getMessage() )
					.contains( rawSuperClass.getName() )
					.contains( "should be 0 or greater" );
		}
		return this;
	}

	public GenericTypeContextAssert typeParameterIndexTooHigh(Class<?> rawSuperClass, int typeArgumentIndex) {
		try {
			getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
			fail( "Expected resolveTypeArgument(" + rawSuperClass + ", " + typeArgumentIndex + ")"
					+ " for type " + getTypeContext() + " to fail because of the invalid index" );
		}
		catch (IllegalArgumentException e) {
			assertThat( e.getMessage() )
					.contains( rawSuperClass.getName() )
					.contains( "only declares " )
					.contains( " type parameter(s)" );
		}
		return this;
	}

	public GenericTypeContextAssert castTo(Class<?> target, Consumer<GenericTypeContextAssert> assertions) {
		assertions.accept( new SimpleGenericTypeContextAssert( getTypeContext().castTo( target ) ) );
		return this;
	}

	public static class SimpleGenericTypeContextAssert extends GenericTypeContextAssert {
		private final GenericTypeContext typeContext;

		public SimpleGenericTypeContextAssert(GenericTypeContext typeContext) {
			this.typeContext = typeContext;
		}

		@Override
		public GenericTypeContext getTypeContext() {
			return typeContext;
		}
	}

	@SuppressWarnings("unused")
	public abstract static class AssertWithType<T> extends GenericTypeContextAssert {
		private final GenericTypeContext typeContext;

		public AssertWithType() {
			typeContext = new GenericTypeContext(
					TypeCapture.captureTypeArgument( AssertWithType.class, this )
			);
		}

		@Override
		public GenericTypeContext getTypeContext() {
			return typeContext;
		}
	}

	/**
	 * Used for wildcard types.
	 * @see WildcardTypeCapture
	 */
	@SuppressWarnings("unused")
	public abstract static class AssertWithWildcardType<T extends WildcardTypeCapture.Of<?>> extends GenericTypeContextAssert {
		private final GenericTypeContext typeContext;

		public AssertWithWildcardType() {
			typeContext = new GenericTypeContext(
					WildcardTypeCapture.captureTypeArgument( AssertWithWildcardType.class, this )
			);
		}

		@Override
		public GenericTypeContext getTypeContext() {
			return typeContext;
		}
	}
}
