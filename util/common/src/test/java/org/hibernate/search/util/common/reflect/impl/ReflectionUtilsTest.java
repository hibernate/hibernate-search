/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.util.impl.test.reflect.TypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture.Of;

import org.junit.jupiter.api.Test;

class ReflectionUtilsTest {

	@Test
	void simple() {
		new AssertWithType<String>() {
		}
				.resolveRawTypeTo( String.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<Iterable<CustomType>>() {
		}
				.resolveRawTypeTo( Iterable.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<Collection<CustomType>>() {
		}
				.resolveRawTypeTo( Collection.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<List<CustomType>>() {
		}
				.resolveRawTypeTo( List.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<ArrayList<CustomType>>() {
		}
				.resolveRawTypeTo( ArrayList.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<Map<String, CustomType>>() {
		}
				.resolveRawTypeTo( Map.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<HashMap<String, CustomType>>() {
		}
				.resolveRawTypeTo( HashMap.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<String[]>() {
		}
				.resolveRawTypeTo( String[].class )
				.resolveArrayElementTypeTo( String.class );
	}

	@Test
	void genericArgument() {
		new AssertWithType<Iterable<CustomGenericType<String, Integer>>>() {
		}
				.resolveRawTypeTo( Iterable.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<CustomGenericType<String, Integer>[]>() {
		}
				.resolveRawTypeTo( CustomGenericType[].class )
				.resolveArrayElementTypeTo( new TypeCapture<CustomGenericType<String, Integer>>() {
				} );
	}

	@Test
	@SuppressWarnings("rawtypes")
	void rawType() {
		new AssertWithType<Iterable>() {
		}
				.resolveRawTypeTo( Iterable.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<CustomGenericType[]>() {
		}
				.resolveRawTypeTo( CustomGenericType[].class )
				.resolveArrayElementTypeTo( CustomGenericType.class );
	}

	@Test
	<T> void unboundedTypeVariable() {
		// Type variable as the tested type
		new AssertWithType<T>() {
		}
				.resolveRawTypeTo( Object.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<T[]>() {
		}
				.resolveRawTypeTo( Object[].class )
				.resolveArrayElementTypeTo( new TypeCapture<T>() {
				} );

		// Type variable as an argument to the tested type
		new AssertWithType<CustomGenericInterface<T, String>>() {
		}
				.resolveRawTypeTo( CustomGenericInterface.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<CustomGenericInterface<T, String>[]>() {
		}
				.resolveRawTypeTo( CustomGenericInterface[].class )
				.resolveArrayElementTypeTo( new TypeCapture<CustomGenericInterface<T, String>>() {
				} );
	}

	@Test
	<T extends CustomGenericInterface<Integer, String>> void singleUpperBoundTypeVariable() {
		// Type variable as the tested type
		new AssertWithType<T>() {
		}
				.resolveRawTypeTo( CustomGenericInterface.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<T[]>() {
		}
				.resolveRawTypeTo( CustomGenericInterface[].class )
				.resolveArrayElementTypeTo( new TypeCapture<T>() {
				} );

		// Type variable as an argument to the tested type
		new AssertWithType<Map<T, String>>() {
		}
				.resolveRawTypeTo( Map.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<Map<T, String>[]>() {
		}
				.resolveRawTypeTo( Map[].class )
				.resolveArrayElementTypeTo( new TypeCapture<Map<T, String>>() {
				} );
	}

	@Test
	<T extends CustomGenericInterface<Integer, String> & Collection<Double>> void multipleUpperBoundsTypeVariable() {
		// Type variable as the tested type
		new AssertWithType<T>() {
		}
				.resolveRawTypeTo( CustomGenericInterface.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<T[]>() {
		}
				.resolveRawTypeTo( CustomGenericInterface[].class )
				.resolveArrayElementTypeTo( new TypeCapture<T>() {
				} );

		// Type variable as an argument to the tested type
		new AssertWithType<Map<T, String>>() {
		}
				.resolveRawTypeTo( Map.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<Map<T, String>[]>() {
		}
				.resolveRawTypeTo( Map[].class )
				.resolveArrayElementTypeTo( new TypeCapture<Map<T, String>>() {
				} );
	}

	@Test
	void unboundedWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<?>>() {
		}
				.resolveRawTypeTo( Object.class )
				.resolveArrayElementTypeToEmpty();

		// Wildcard as an argument to the tested type
		new AssertWithType<CustomGenericInterface<?, String>>() {
		}
				.resolveRawTypeTo( CustomGenericInterface.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<CustomGenericInterface<?, String>[]>() {
		}
				.resolveRawTypeTo( CustomGenericInterface[].class )
				.resolveArrayElementTypeTo( new TypeCapture<CustomGenericInterface<?, String>>() {
				} );
	}

	@Test
	void singleUpperBoundWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<? extends CustomGenericInterface<Integer, String>>>() {
		}
				.resolveRawTypeTo( CustomGenericInterface.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithWildcardType<Of<? extends CustomGenericInterface<Integer, String>[]>>() {
		}
				.resolveRawTypeTo( CustomGenericInterface[].class )
				.resolveArrayElementTypeTo( new TypeCapture<CustomGenericInterface<Integer, String>>() {
				} );

		// Wildcard as an argument to the tested type
		new AssertWithType<Map<? extends CustomGenericInterface<Integer, String>, String>>() {
		}
				.resolveRawTypeTo( Map.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<Map<? extends CustomGenericInterface<Integer, String>, String>[]>() {
		}
				.resolveRawTypeTo( Map[].class )
				.resolveArrayElementTypeTo( new TypeCapture<Map<? extends CustomGenericInterface<Integer, String>, String>>() {
				} );
	}

	@Test
	<T> void unboundedTypeVariableUpperBoundWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<? extends T>>() {
		}
				.resolveRawTypeTo( Object.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithWildcardType<Of<? extends T[]>>() {
		}
				.resolveRawTypeTo( Object[].class )
				.resolveArrayElementTypeTo( new TypeCapture<T>() {
				} );

		// Wildcard as an argument to the tested type
		new AssertWithType<CustomGenericInterface<? extends T, String>>() {
		}
				.resolveRawTypeTo( CustomGenericInterface.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<CustomGenericInterface<? extends T, String>[]>() {
		}
				.resolveRawTypeTo( CustomGenericInterface[].class )
				.resolveArrayElementTypeTo( new TypeCapture<CustomGenericInterface<? extends T, String>>() {
				} );
	}

	@Test
	<T extends CustomGenericInterface<Integer, String>> void boundedTypeVariableUpperBoundWildcard() {
		// Wildcard as the tested type
		new AssertWithWildcardType<Of<? extends T>>() {
		}
				.resolveRawTypeTo( CustomGenericInterface.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithWildcardType<Of<? extends T[]>>() {
		}
				.resolveRawTypeTo( CustomGenericInterface[].class )
				.resolveArrayElementTypeTo( new TypeCapture<T>() {
				} );

		// Wildcard as an argument to the tested type
		new AssertWithType<Map<? extends T, String>>() {
		}
				.resolveRawTypeTo( Map.class )
				.resolveArrayElementTypeToEmpty();
		new AssertWithType<Map<? extends T, String>[]>() {
		}
				.resolveRawTypeTo( Map[].class )
				.resolveArrayElementTypeTo( new TypeCapture<Map<? extends T, String>>() {
				} );
	}

	private abstract static class AbstractReflectionUtilsForTypeAssert {
		abstract Type getType();

		AbstractReflectionUtilsForTypeAssert resolveRawTypeTo(Class<?> expected) {
			Class<?> rawType = ReflectionUtils.getRawType( getType() );
			assertThat( rawType )
					.as( "Unexpected raw type for " + getType() )
					.isEqualTo( expected );
			return this;
		}

		AbstractReflectionUtilsForTypeAssert resolveArrayElementTypeTo(TypeCapture<?> expected) {
			return resolveArrayElementTypeTo( expected.getType() );
		}

		AbstractReflectionUtilsForTypeAssert resolveArrayElementTypeTo(Type expected) {
			Optional<Type> optional = ReflectionUtils.getArrayElementType( getType() );
			assertThat( optional.isPresent() )
					.as( "Expected " + getType() + " to be considered an array of " + expected )
					.isTrue();
			assertThat( optional.get() )
					.isEqualTo( expected );
			return this;
		}

		AbstractReflectionUtilsForTypeAssert resolveArrayElementTypeToEmpty() {
			Optional<Type> optional = ReflectionUtils.getArrayElementType( getType() );
			assertThat( optional.isPresent() )
					.as( "Expected " + getType() + " NOT to be considered an array" )
					.isFalse();
			return this;
		}
	}

	@SuppressWarnings("unused")
	private abstract static class AssertWithType<T> extends AbstractReflectionUtilsForTypeAssert {
		private final Type type;

		AssertWithType() {
			type = TypeCapture.captureTypeArgument( AssertWithType.class, this );
		}

		@Override
		Type getType() {
			return type;
		}
	}

	/**
	 * Used for wildcard types.
	 * @see WildcardTypeCapture
	 */
	@SuppressWarnings("unused")
	private abstract static class AssertWithWildcardType<T extends Of<?>> extends AbstractReflectionUtilsForTypeAssert {
		private final Type type;

		AssertWithWildcardType() {
			type = WildcardTypeCapture.captureTypeArgument( AssertWithWildcardType.class, this );
		}

		@Override
		Type getType() {
			return type;
		}
	}

}
