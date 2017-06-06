/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides some methods for simplified collection instantiation.
 *
 * @author Gunnar Morling
 * @author Hardy Ferentschik
 */
public final class CollectionHelper {

	private CollectionHelper() {
		// not allowed
	}

	public static <K, V> HashMap<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <K, V> HashMap<K, V> newHashMap(int size) {
		return new HashMap<K, V>( size );
	}

	public static <K, V> SortedMap<K, V> newSortedMap() {
		return new TreeMap<K, V>();
	}

	public static <T> HashSet<T> newHashSet() {
		return new HashSet<T>();
	}

	public static <T> ArrayList<T> newArrayList() {
		return new ArrayList<T>();
	}

	public static <T> ArrayList<T> newArrayList(final int size) {
		return new ArrayList<T>( size );
	}

	public static <T> Set<T> asSet(T... ts) {
		HashSet<T> set = new HashSet<T>( ts.length );
		Collections.addAll( set, ts );
		return set;
	}

	public static <T> List<T> toImmutableList(final Collection<? extends T> c) {
		if ( c.isEmpty() ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( new ArrayList<T>( c ) );
		}
	}

	public static Set<String> asImmutableSet(String[] names) {
		//The intention here is to save some memory by picking the simplest safe representation,
		// as we usually require immutable sets for long living metadata:
		if ( names == null || names.length == 0 ) {
			return Collections.<String>emptySet();
		}
		else if ( names.length == 1 ) {
			return Collections.singleton( names[0] );
		}
		else {
			HashSet<String> hashSet = new HashSet<>( Arrays.asList( names ) );
			return Collections.unmodifiableSet( hashSet );
		}
	}

	/**
	 * Builds an {@link Iterator} for a given array. It is (un)necessarily ugly because we have to deal with array of primitives.
	 *
	 * @param object a given array
	 * @return an {@code Iterator} iterating over the array
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" }) // Reflection is used to ensure the correct types are used
	public static Iterator<?> iteratorFromArray(Object object) {
		return new ArrayIterator( accessorFromArray( object ), object );
	}

	/**
	 * Builds an {@link Iterable} for a given array. It is (un)necessarily ugly because we have to deal with array of primitives.
	 *
	 * @param object a given array
	 * @return an {@code Iterable} providing iterators over the array
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" }) // Reflection is used to ensure the correct types are used
	public static Iterable<?> iterableFromArray(Object object) {
		return new ArrayIterable( accessorFromArray( object ), object );
	}

	public static <T> Iterable<T> flatten(Iterable<? extends Iterable<T>> nonFlat) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return flatten( nonFlat.iterator() );
			}
		};
	}

	public static <T> Iterator<T> flatten(Iterator<? extends Iterable<T>> nonFlat) {
		return new FlatteningIterator<>( nonFlat );
	}

	private static ArrayAccessor<?, ?> accessorFromArray(Object object) {
		ArrayAccessor<?, ?> accessor;
		if ( Object.class.isAssignableFrom( object.getClass().getComponentType() ) ) {
			accessor = ArrayAccessor.OBJECT;
		}
		else if ( object.getClass() == boolean[].class ) {
			accessor = ArrayAccessor.BOOLEAN;
		}
		else if ( object.getClass() == int[].class ) {
			accessor = ArrayAccessor.INT;
		}
		else if ( object.getClass() == long[].class ) {
			accessor = ArrayAccessor.LONG;
		}
		else if ( object.getClass() == double[].class ) {
			accessor = ArrayAccessor.DOUBLE;
		}
		else if ( object.getClass() == float[].class ) {
			accessor = ArrayAccessor.FLOAT;
		}
		else if ( object.getClass() == byte[].class ) {
			accessor = ArrayAccessor.BYTE;
		}
		else if ( object.getClass() == short[].class ) {
			accessor = ArrayAccessor.SHORT;
		}
		else if ( object.getClass() == char[].class ) {
			accessor = ArrayAccessor.CHAR;
		}
		else {
			throw new IllegalArgumentException( "Provided object " + object + " is not a supported array type" );
		}
		return accessor;
	}

	private static class ArrayIterable<A, T> implements Iterable<T> {
		private final ArrayAccessor<A, T> accessor;
		private final A values;

		public ArrayIterable(ArrayAccessor<A, T> accessor, A values) {
			this.accessor = accessor;
			this.values = values;
		}

		@Override
		public final Iterator<T> iterator() {
			return new ArrayIterator<>( accessor, values );
		}
	}

	private static class ArrayIterator<A, T> implements Iterator<T> {
		private final ArrayAccessor<A, T> accessor;
		private final A values;
		private int current = 0;

		public ArrayIterator(ArrayAccessor<A, T> accessor, A values) {
			this.accessor = accessor;
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < accessor.size( values );
		}

		@Override
		public T next() {
			T result = accessor.get( values, current );
			current++;
			return result;
		}
	}

	private interface ArrayAccessor<A, T> {

		int size(A array);

		T get(A array, int index);

		ArrayAccessor<Object[], Object> OBJECT = new ArrayAccessor<Object[], Object>() {
			@Override
			public int size(Object[] array) {
				return array.length;
			}

			@Override
			public Object get(Object[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<boolean[], Boolean> BOOLEAN = new ArrayAccessor<boolean[], Boolean>() {
			@Override
			public int size(boolean[] array) {
				return array.length;
			}

			@Override
			public Boolean get(boolean[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<int[], Integer> INT = new ArrayAccessor<int[], Integer>() {
			@Override
			public int size(int[] array) {
				return array.length;
			}

			@Override
			public Integer get(int[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<long[], Long> LONG = new ArrayAccessor<long[], Long>() {
			@Override
			public int size(long[] array) {
				return array.length;
			}

			@Override
			public Long get(long[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<double[], Double> DOUBLE = new ArrayAccessor<double[], Double>() {
			@Override
			public int size(double[] array) {
				return array.length;
			}

			@Override
			public Double get(double[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<float[], Float> FLOAT = new ArrayAccessor<float[], Float>() {
			@Override
			public int size(float[] array) {
				return array.length;
			}

			@Override
			public Float get(float[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<byte[], Byte> BYTE = new ArrayAccessor<byte[], Byte>() {
			@Override
			public int size(byte[] array) {
				return array.length;
			}

			@Override
			public Byte get(byte[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<short[], Short> SHORT = new ArrayAccessor<short[], Short>() {
			@Override
			public int size(short[] array) {
				return array.length;
			}

			@Override
			public Short get(short[] array, int index) {
				return array[index];
			}
		};

		ArrayAccessor<char[], Character> CHAR = new ArrayAccessor<char[], Character>() {
			@Override
			public int size(char[] array) {
				return array.length;
			}

			@Override
			public Character get(char[] array, int index) {
				return array[index];
			}
		};
	}

	private static class FlatteningIterator<T> implements Iterator<T> {

		private Iterator<? extends Iterable<T>> nonFlatIterator;
		private Iterator<T> current = Collections.<T>emptyIterator();

		public FlatteningIterator(Iterator<? extends Iterable<T>> nonFlatIterator) {
			this.nonFlatIterator = nonFlatIterator;
		}

		@Override
		public boolean hasNext() {
			// Get the next iterable if we iterated through all elements of the current one
			while ( ! current.hasNext() && nonFlatIterator.hasNext() ) {
				current = nonFlatIterator.next().iterator();
			}
			return current.hasNext() || nonFlatIterator.hasNext();
		}

		@Override
		public T next() {
			// force the position to an non empty current or the end of the flow
			if ( ! hasNext() ) {
				throw new NoSuchElementException();
			}
			return current.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}
