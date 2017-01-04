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
	public static Iterator<?> iteratorFromArray(Object object) {
		Iterator<?> iterator;
		if ( Object.class.isAssignableFrom( object.getClass().getComponentType() ) ) {
			iterator = new ObjectArrayIterator( (Object[]) object );
		}
		else if ( object.getClass() == boolean[].class ) {
			iterator = new BooleanArrayIterator( (boolean[]) object );
		}
		else if ( object.getClass() == int[].class ) {
			iterator = new IntArrayIterator( (int[]) object );
		}
		else if ( object.getClass() == long[].class ) {
			iterator = new LongArrayIterator( (long[]) object );
		}
		else if ( object.getClass() == double[].class ) {
			iterator = new DoubleArrayIterator( (double[]) object );
		}
		else if ( object.getClass() == float[].class ) {
			iterator = new FloatArrayIterator( (float[]) object );
		}
		else if ( object.getClass() == byte[].class ) {
			iterator = new ByteArrayIterator( (byte[]) object );
		}
		else if ( object.getClass() == short[].class ) {
			iterator = new ShortArrayIterator( (short[]) object );
		}
		else if ( object.getClass() == char[].class ) {
			iterator = new CharArrayIterator( (char[]) object );
		}
		else {
			throw new IllegalArgumentException( "Provided object " + object + " is not a supported array type" );
		}
		return iterator;
	}

	private static class ObjectArrayIterator implements Iterator<Object> {

		private Object[] values;
		private int current = 0;

		private ObjectArrayIterator(Object[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Object next() {
			Object result = values[current];
			current++;
			return result;
		}
	}

	private static class BooleanArrayIterator implements Iterator<Boolean> {

		private boolean[] values;
		private int current = 0;

		private BooleanArrayIterator(boolean[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Boolean next() {
			boolean result = values[current];
			current++;
			return result;
		}
	}

	private static class IntArrayIterator implements Iterator<Integer> {

		private int[] values;
		private int current = 0;

		private IntArrayIterator(int[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Integer next() {
			int result = values[current];
			current++;
			return result;
		}
	}

	private static class LongArrayIterator implements Iterator<Long> {

		private long[] values;
		private int current = 0;

		private LongArrayIterator(long[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Long next() {
			long result = values[current];
			current++;
			return result;
		}
	}

	private static class DoubleArrayIterator implements Iterator<Double> {

		private double[] values;
		private int current = 0;

		private DoubleArrayIterator(double[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Double next() {
			double result = values[current];
			current++;
			return result;
		}
	}

	private static class FloatArrayIterator implements Iterator<Float> {

		private float[] values;
		private int current = 0;

		private FloatArrayIterator(float[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Float next() {
			float result = values[current];
			current++;
			return result;
		}
	}

	private static class ByteArrayIterator implements Iterator<Byte> {

		private byte[] values;
		private int current = 0;

		private ByteArrayIterator(byte[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Byte next() {
			byte result = values[current];
			current++;
			return result;
		}
	}

	private static class ShortArrayIterator implements Iterator<Short> {

		private short[] values;
		private int current = 0;

		private ShortArrayIterator(short[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Short next() {
			short result = values[current];
			current++;
			return result;
		}
	}

	private static class CharArrayIterator implements Iterator<Character> {

		private char[] values;
		private int current = 0;

		private CharArrayIterator(char[] values) {
			this.values = values;
		}

		@Override
		public boolean hasNext() {
			return current < values.length;
		}

		@Override
		public Character next() {
			char result = values[current];
			current++;
			return result;
		}
	}
}
