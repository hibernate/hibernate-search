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

	public static <T> List<T> toImmutableList(final Collection<T> c) {
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
	public static Iterator<?> iteratorFromArray(Object object) {
		return iterableFromArray( object ).iterator();
	}

	/**
	 * Builds an {@link Iterable} for a given array. It is (un)necessarily ugly because we have to deal with array of primitives.
	 *
	 * @param object a given array
	 * @return an {@code Iterable} providing iterators over the array
	 */
	public static Iterable<?> iterableFromArray(Object object) {
		Iterable<?> iterable;
		if ( Object.class.isAssignableFrom( object.getClass().getComponentType() ) ) {
			iterable = new ObjectArrayIterable( (Object[]) object );
		}
		else if ( object.getClass() == boolean[].class ) {
			iterable = new BooleanArrayIterable( (boolean[]) object );
		}
		else if ( object.getClass() == int[].class ) {
			iterable = new IntArrayIterable( (int[]) object );
		}
		else if ( object.getClass() == long[].class ) {
			iterable = new LongArrayIterable( (long[]) object );
		}
		else if ( object.getClass() == double[].class ) {
			iterable = new DoubleArrayIterable( (double[]) object );
		}
		else if ( object.getClass() == float[].class ) {
			iterable = new FloatArrayIterable( (float[]) object );
		}
		else if ( object.getClass() == byte[].class ) {
			iterable = new ByteArrayIterable( (byte[]) object );
		}
		else if ( object.getClass() == short[].class ) {
			iterable = new ShortArrayIterable( (short[]) object );
		}
		else if ( object.getClass() == char[].class ) {
			iterable = new CharArrayIterable( (char[]) object );
		}
		else {
			throw new IllegalArgumentException( "Provided object " + object + " is not a supported array type" );
		}
		return iterable;
	}

	private abstract static class ArrayIterable<T> implements Iterable<T> {

		protected abstract int size();

		protected abstract T get(int index);

		@Override
		public final Iterator<T> iterator() {
			return new ArrayIterator();
		}

		private class ArrayIterator implements Iterator<T> {
			private int current = 0;

			@Override
			public boolean hasNext() {
				return current < size();
			}

			@Override
			public T next() {
				T result = get( current );
				current++;
				return result;
			}
		}
	}

	private static class ObjectArrayIterable extends ArrayIterable<Object> {

		private Object[] values;

		private ObjectArrayIterable(Object[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Object get(int index) {
			return values[index];
		}
	}

	private static class BooleanArrayIterable extends ArrayIterable<Boolean> {

		private boolean[] values;

		private BooleanArrayIterable(boolean[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Boolean get(int index) {
			return values[index];
		}
	}

	private static class IntArrayIterable extends ArrayIterable<Integer> {

		private int[] values;

		private IntArrayIterable(int[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Integer get(int index) {
			return values[index];
		}
	}

	private static class LongArrayIterable extends ArrayIterable<Long> {

		private long[] values;

		private LongArrayIterable(long[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Long get(int index) {
			return values[index];
		}
	}

	private static class DoubleArrayIterable extends ArrayIterable<Double> {

		private double[] values;

		private DoubleArrayIterable(double[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Double get(int index) {
			return values[index];
		}
	}

	private static class FloatArrayIterable extends ArrayIterable<Float> {

		private float[] values;

		private FloatArrayIterable(float[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Float get(int index) {
			return values[index];
		}
	}

	private static class ByteArrayIterable extends ArrayIterable<Byte> {

		private byte[] values;

		private ByteArrayIterable(byte[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Byte get(int index) {
			return values[index];
		}
	}

	private static class ShortArrayIterable extends ArrayIterable<Short> {

		private short[] values;

		private ShortArrayIterable(short[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Short get(int index) {
			return values[index];
		}
	}

	private static class CharArrayIterable extends ArrayIterable<Character> {

		private char[] values;

		private CharArrayIterable(char[] values) {
			this.values = values;
		}

		@Override
		protected int size() {
			return values.length;
		}

		@Override
		protected Character get(int index) {
			return values[index];
		}
	}
}
