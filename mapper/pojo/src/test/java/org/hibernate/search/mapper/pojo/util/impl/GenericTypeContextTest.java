/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.util.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.test.util.TypeCapture;
import org.hibernate.search.mapper.pojo.test.util.WildcardTypeCapture;
import org.hibernate.search.mapper.pojo.test.util.WildcardTypeCapture.Of;
import org.hibernate.search.util.SearchException;

import org.junit.Test;

public class GenericTypeContextTest {

	@Test
	public void simple() {
		new AssertWithType<Iterable<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new AssertWithType<Collection<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Collection.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new AssertWithType<List<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Collection.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new AssertWithType<ArrayList<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Collection.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, List.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, ArrayList.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new AssertWithType<Map<String, CustomType>>() {
		}
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Map.class, 1 );
		new AssertWithType<HashMap<String, CustomType>>() {
		}
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Map.class, 1 );
	}

	@Test
	public void genericArgument() {
		new AssertWithType<Iterable<CustomGenericType<String, Integer>>>() {
		}
				.resolveTypeArgumentTo( new TypeCapture<CustomGenericType<String, Integer>>() {
				}, Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
	}

	@Test
	public void rawType() {
		new AssertWithType<Collection>() {
		}
				.resolveTypeArgumentTo(
						Collection.class.getTypeParameters()[0],
						Iterable.class, 0
				)
				.resolveTypeArgumentTo(
						Collection.class.getTypeParameters()[0],
						Collection.class, 0
				)
				.resolveTypeArgumentToEmpty( List.class, 0 );
	}

	@Test
	public void boundedRawType() {
		new AssertWithType<CustomBoundedGenericInterface>() {
		}
				.resolveTypeArgumentTo(
						CustomBoundedGenericInterface.class.getTypeParameters()[0],
						CustomBoundedGenericInterface.class, 0
				);
	}

	@Test
	public void swappedArguments() {
		new AssertWithType<CustomGenericType<String, Integer>>() {
		}
				.resolveTypeArgumentTo( String.class, CustomGenericType.class, 0 )
				.resolveTypeArgumentTo( Integer.class, CustomGenericType.class, 1 )
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 );
	}

	@Test
	public void fixedArguments() {
		new AssertWithType<CustomArgumentSettingType>() {
		}
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, CustomGenericInterface.class, 1 );
	}

	@Test
	public <T> void unboundedTypeVariable() {
		// Type variable as the tested type
		new AssertWithType<T>() {
		}
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Type variable as an argument to the tested type
		new AssertWithType<Map<T, String>>() {
		}
				.resolveTypeArgumentTo( new TypeCapture<T>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public <T extends CustomGenericInterface<Integer, String>> void singleUpperBoundTypeVariable() {
		// Type variable as the tested type
		new AssertWithType<T>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Type variable as an argument to the tested type
		new AssertWithType<Map<T, String>>() {
		}
				.resolveTypeArgumentTo( new TypeCapture<T>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public <T extends CustomGenericInterface<Integer, String> & Collection<Double>> void multipleUpperBoundsTypeVariable() {
		// Type variable as the tested type
		new AssertWithType<T>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentTo( Double.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( Double.class, Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Type variable as an argument to the tested type
		new AssertWithType<Map<T, String>>() {
		}
				.resolveTypeArgumentTo( new TypeCapture<T>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public void unboundedWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<?>>() {
		}
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new AssertWithType<CustomGenericType<?, String>>() {
		}
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<?>>() {
				}, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
	}

	@Test
	public void singleUpperBoundWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<? extends CustomGenericInterface<Integer, String>>>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new AssertWithType<Map<? extends CustomGenericInterface<Integer, String>, String>>() {
		}
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<? extends CustomGenericInterface<Integer, String>>>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public <T> void unboundedTypeVariableUpperBoundWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<? extends T>>() {
		}
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new AssertWithType<CustomGenericInterface<? extends T, String>>() {
		}
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<? extends T>>() {
				}, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
	}

	@Test
	public <T extends CustomGenericInterface<Integer, String>> void boundedTypeVariableUpperBoundWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<? extends T>>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new AssertWithType<Map<? extends T, String>>() {
		}
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<? extends T>>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public void badIndex() {
		new AssertWithType<CustomGenericType<String, Integer>>() {
		}
				.typeParameterIndexTooHigh( CustomGenericInterface.class, 2 )
				.typeParameterIndexTooHigh( CustomGenericInterface.class, 42 )
				.typeParameterIndexTooHigh( CustomGenericInterface.class, Integer.MAX_VALUE )
				.typeParameterIndexTooHigh( Collection.class, 1 )
				.typeParameterIndexTooLow( Collection.class, -1 )
				.typeParameterIndexTooLow( Collection.class, Integer.MIN_VALUE )
				.noTypeParameter( String.class, 0 )
				.noTypeParameter( CustomArgumentSettingType.class, 0 );
	}

	private abstract static class AbstractGenericTypeContextAssert {
		abstract GenericTypeContext getTypeContext();

		AbstractGenericTypeContextAssert resolveTypeArgumentTo(TypeCapture<?> expected, Class<?> rawSuperClass, int typeArgumentIndex) {
			return resolveTypeArgumentTo( expected.getType(), rawSuperClass, typeArgumentIndex );
		}

		AbstractGenericTypeContextAssert resolveTypeArgumentTo(Type expected, Class<?> rawSuperClass, int typeArgumentIndex) {
			Optional<Type> optional = getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
			assertThat( optional.isPresent() )
					.as( "Expected " + getTypeContext() + " to be considered a subtype of " + rawSuperClass )
					.isTrue();
			assertThat( optional.get() )
					.isEqualTo( expected );
			return this;
		}

		AbstractGenericTypeContextAssert resolveTypeArgumentToEmpty(Class<?> rawSuperClass, int typeArgumentIndex) {
			Optional<?> optional = getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
			assertThat( optional.isPresent() )
					.as( "Expected " + getTypeContext() + " NOT to be considered a subtype of " + rawSuperClass )
					.isFalse();
			return this;
		}

		AbstractGenericTypeContextAssert noTypeParameter(Class<?> rawSuperClass, int typeArgumentIndex) {
			try {
				getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
				fail( "Expected resolveTypeArgument(" + rawSuperClass + ", " + typeArgumentIndex + ")"
						+ " for type " + getTypeContext() + " to fail because " + rawSuperClass + " doesn't have any type parameter" );
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.contains( rawSuperClass.getName() )
						.contains( "doesn't declare any type parameter" );
			}
			return this;
		}

		AbstractGenericTypeContextAssert typeParameterIndexTooLow(Class<?> rawSuperClass, int typeArgumentIndex) {
			try {
				getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
				fail( "Expected resolveTypeArgument(" + rawSuperClass + ", " + typeArgumentIndex + ")"
						+ " for type " + getTypeContext() + " to fail because of the invalid index" );
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.contains( rawSuperClass.getName() )
						.contains( "should be 0 or greater" );
			}
			return this;
		}

		AbstractGenericTypeContextAssert typeParameterIndexTooHigh(Class<?> rawSuperClass, int typeArgumentIndex) {
			try {
				getTypeContext().resolveTypeArgument( rawSuperClass, typeArgumentIndex );
				fail( "Expected resolveTypeArgument(" + rawSuperClass + ", " + typeArgumentIndex + ")"
						+ " for type " + getTypeContext() + " to fail because of the invalid index" );
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.contains( rawSuperClass.getName() )
						.contains( "only declares " )
						.contains( " type parameter(s)" );
			}
			return this;
		}
	}

	@SuppressWarnings("unused")
	private abstract static class AssertWithType<T> extends AbstractGenericTypeContextAssert {
		private final GenericTypeContext typeContext;

		AssertWithType() {
			typeContext = new GenericTypeContext(
					TypeCapture.captureTypeArgument( AssertWithType.class, this )
			);
		}

		@Override
		GenericTypeContext getTypeContext() {
			return typeContext;
		}
	}

	/**
	 * Used for wildcard types.
	 * @see WildcardTypeCapture
	 */
	@SuppressWarnings("unused")
	private abstract static class AssertWithWildcardType<T extends Of<?>> extends AbstractGenericTypeContextAssert {
		private final GenericTypeContext typeContext;

		AssertWithWildcardType() {
			typeContext = new GenericTypeContext(
					WildcardTypeCapture.captureTypeArgument( AssertWithWildcardType.class, this )
			);
		}

		@Override
		GenericTypeContext getTypeContext() {
			return typeContext;
		}
	}
}
