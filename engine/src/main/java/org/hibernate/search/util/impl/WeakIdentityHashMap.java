/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.util.impl;


import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A hashtable-based <tt>Map</tt> implementation with <em>weak keys</em> and
 * using reference-equality in place of object-equality when comparing keys
 * (and values).  In an <tt>WeakIdentityHashMap</tt>, two keys <tt>k1</tt> and
 * <tt>k2</tt> are considered equal if and only if <tt>(k1==k2)</tt>.
 * An entry in a <tt>WeakIdentityHashMap</tt> will automatically be removed when
 * its key is no longer in ordinary use.  More precisely, the presence of a
 * mapping for a given key will not prevent the key from being discarded by the
 * garbage collector, that is, made finalizable, finalized, and then reclaimed.
 * When a key has been discarded its entry is effectively removed from the map.
 * <p/>
 * <p>Based on java.util.WeakHashMap</p>
 * <p>Based on org.jboss.common.collection.WeakIdentityHashMap</p>
 *
 * @author Dawid Kurzyniec
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Emmanuel Bernard
 * @see		java.util.IdentityHashMap
 * @see		java.util.WeakHashMap
 */
public class WeakIdentityHashMap<K,V> /*extends AbstractMap*/ implements Map<K,V> {

	/**
	 * Value representing null keys inside tables.
	 */
	private static final Object NULL_KEY = new Object();

	/**
	 * The default initial capacity -- MUST be a power of two.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two <= 1<<30.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load fast used when none specified in constructor.
	 */
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * The table, resized as necessary. Length MUST Always be a power of two.
	 */
	private Entry<K,V>[] table;

	/**
	 * The number of key-value mappings contained in this weak hash map.
	 */
	private int size;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 */
	private int threshold;

	/**
	 * The load factor for the hash table.
	 */
	private final float loadFactor;

	/**
	 * Reference queue for cleared WeakEntries
	 */
	private final ReferenceQueue queue = new ReferenceQueue();

	/**
	 * The number of times this HashMap has been structurally modified
	 * Structural modifications are those that change the number of mappings in
	 * the HashMap or otherwise modify its internal structure (e.g.,
	 * rehash).  This field is used to make iterators on Collection-views of
	 * the HashMap fail-fast.  (See ConcurrentModificationException).
	 */
	private volatile int modCount;

	/**
	 * Each of these fields are initialized to contain an instance of the
	 * appropriate view the first time this view is requested.  The views are
	 * stateless, so there's no reason to create more than one of each.
	 */
	private transient volatile Set keySet = null;
	private transient volatile Collection values = null;
	private transient Set<Map.Entry<K,V>> entrySet = null;


	/**
	 * Constructs a new, empty <tt>WeakIdentityHashMap</tt> with the given
	 * initial capacity and the given load factor.
	 *
	 * @param initialCapacity The initial capacity of the
	 *                        <tt>WeakIdentityHashMap</tt>
	 * @param loadFactor	  The load factor of the
	 *                        <tt>WeakIdentityHashMap</tt>
	 * @throws IllegalArgumentException If the initial capacity is negative,
	 *                                  or if the load factor is nonpositive.
	 */
	public WeakIdentityHashMap(int initialCapacity, float loadFactor) {
		if ( initialCapacity < 0 ) {
			throw new IllegalArgumentException( "Illegal Initial Capacity: " + initialCapacity );
		}
		if ( initialCapacity > MAXIMUM_CAPACITY ) {
			initialCapacity = MAXIMUM_CAPACITY;
		}

		if ( loadFactor <= 0 || Float.isNaN( loadFactor ) ) {
			throw new IllegalArgumentException( "Illegal Load factor: " + loadFactor );
		}
		int capacity = 1;
		while ( capacity < initialCapacity ) {
			capacity <<= 1;
		}
		table = new Entry[capacity];
		this.loadFactor = loadFactor;
		threshold = (int) ( capacity * loadFactor );
	}

	/**
	 * Constructs a new, empty <tt>WeakIdentityHashMap</tt> with the given
	 * initial capacity and the default load factor, which is <tt>0.75</tt>.
	 *
	 * @param initialCapacity The initial capacity of the
	 *                        <tt>WeakIdentityHashMap</tt>
	 * @throws IllegalArgumentException If the initial capacity is negative.
	 */
	public WeakIdentityHashMap(int initialCapacity) {
		this( initialCapacity, DEFAULT_LOAD_FACTOR );
	}

	/**
	 * Constructs a new, empty <tt>WeakIdentityHashMap</tt> with the default
	 * initial capacity (16) and the default load factor (0.75).
	 */
	public WeakIdentityHashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		threshold = DEFAULT_INITIAL_CAPACITY;
		table = new Entry[DEFAULT_INITIAL_CAPACITY];
	}

	/**
	 * Constructs a new <tt>WeakIdentityHashMap</tt> with the same mappings as
	 * the specified <tt>Map</tt>.  The <tt>WeakIdentityHashMap</tt> is created
	 * with default load factor, which is <tt>0.75</tt> and an initial capacity
	 * sufficient to hold the mappings in the specified <tt>Map</tt>.
	 *
	 * @param t the map whose mappings are to be placed in this map.
	 * @throws java.lang.NullPointerException if the specified map is null.
	 */
	public WeakIdentityHashMap(Map t) {
		this( Math.max( (int) ( t.size() / DEFAULT_LOAD_FACTOR ) + 1, 16 ),
				DEFAULT_LOAD_FACTOR );
		putAll( t );
	}

	// internal utilities

	/**
	 * Use NULL_KEY for key if it is null.
	 */
	private static <T> T maskNull(T key) {
		return ( key == null ?
				(T) NULL_KEY : //i don't think there is a better way
				key );
	}

	/**
	 * Return internal representation of null key back to caller as null
	 */
	private static <T> T unmaskNull(T key) {
		return ( key == NULL_KEY ?
				null :
				key );
	}

	/**
	 * Return a hash code for non-null Object x.
	 */
	int hash(Object x) {
		int h = System.identityHashCode( x );
		return h - ( h << 7 ); // that is,, -127 * h
	}

	/**
	 * Return index for hash code h.
	 */
	static int indexFor(int h, int length) {
		return h & ( length - 1 );
	}

	/**
	 * Expunge stale entries from the table.
	 */
	private void expungeStaleEntries() {
		Object r;
		while ( ( r = queue.poll() ) != null ) {
			Entry e = (Entry) r;
			int h = e.hash;
			int i = indexFor( h, table.length );

			Entry prev = table[i];
			Entry p = prev;
			while ( p != null ) {
				Entry next = p.next;
				if ( p == e ) {
					if ( prev == e ) {
						table[i] = next;
					}
					else {
						prev.next = next;
					}
					// Assign null helps GC
					e.next = null;
					e.value = null;
					size--;
					break;
				}
				prev = p;
				p = next;
			}
		}
	}

	/**
	 * Return the table after first expunging stale entries
	 */
	private Entry<K,V>[] getTable() {
		expungeStaleEntries();
		return table;
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 * This result is a snapshot, and may not reflect unprocessed
	 * entries that will be removed before next attempted access
	 * because they are no longer referenced.
	 */
	@Override
	public int size() {
		if ( size == 0 ) {
			return 0;
		}
		expungeStaleEntries();
		return size;
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 * This result is a snapshot, and may not reflect unprocessed
	 * entries that will be removed before next attempted access
	 * because they are no longer referenced.
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Returns the value to which the specified key is mapped in this weak
	 * hash map, or <tt>null</tt> if the map contains no mapping for
	 * this key.  A return value of <tt>null</tt> does not <i>necessarily</i>
	 * indicate that the map contains no mapping for the key; it is also
	 * possible that the map explicitly maps the key to <tt>null</tt>. The
	 * <tt>containsKey</tt> method may be used to distinguish these two
	 * cases.
	 *
	 * @param key the key whose associated value is to be returned.
	 * @return the value to which this map maps the specified key, or
	 *         <tt>null</tt> if the map contains no mapping for this key.
	 * @see #put(Object,Object)
	 */
	@Override
	public V get(Object key) {
		Object k = maskNull( key );
		int h = hash( k );
		Entry<K,V>[] tab = getTable();
		int index = indexFor( h, tab.length );
		Entry<K,V> e = tab[index];
		while ( e != null ) {
			if ( e.hash == h && k == e.get() ) {
				return e.value;
			}
			e = e.next;
		}
		return null;
	}

	/**
	 * Returns <tt>true</tt> if this map contains a mapping for the
	 * specified key.
	 *
	 * @param key The key whose presence in this map is to be tested
	 * @return <tt>true</tt> if there is a mapping for <tt>key</tt>;
	 *         <tt>false</tt> otherwise
	 */
	@Override
	public boolean containsKey(Object key) {
		return getEntry( key ) != null;
	}

	/**
	 * Returns the entry associated with the specified key in the HashMap.
	 * Returns null if the HashMap contains no mapping for this key.
	 */
	Entry<K,V> getEntry(Object key) {
		Object k = maskNull( key );
		int h = hash( k );
		Entry<K,V>[] tab = getTable();
		int index = indexFor( h, tab.length );
		Entry<K,V> e = tab[index];
		while ( e != null && !( e.hash == h && k == e.get() ) ) {
			e = e.next;
		}
		return e;
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for this key, the old
	 * value is replaced.
	 *
	 * @param key   key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *         if there was no mapping for key.  A <tt>null</tt> return can
	 *         also indicate that the HashMap previously associated
	 *         <tt>null</tt> with the specified key.
	 */
	@Override
	public V put(K key, V value) {
		K k = maskNull( key );
		int h = hash( k );
		Entry<K,V>[] tab = getTable();
		int i = indexFor( h, tab.length );

		for ( Entry<K,V> e = tab[i]; e != null; e = e.next ) {
			if ( h == e.hash && k == e.get() ) {
				V oldValue = e.value;
				if ( value != oldValue ) {
					e.value = value;
				}
				return oldValue;
			}
		}

		modCount++;
		tab[i] = new Entry<K,V>( k, value, queue, h, tab[i] );
		if ( ++size >= threshold ) {
			resize( tab.length * 2 );
		}
		return null;
	}

	/**
	 * Rehashes the contents of this map into a new <tt>HashMap</tt> instance
	 * with a larger capacity. This method is called automatically when the
	 * number of keys in this map exceeds its capacity and load factor.
	 * <p/>
	 * Note that this method is a no-op if it's called with newCapacity ==
	 * 2*MAXIMUM_CAPACITY (which is Integer.MIN_VALUE).
	 *
	 * @param newCapacity the new capacity, MUST be a power of two.
	 */
	void resize(int newCapacity) {
		// assert (newCapacity & -newCapacity) == newCapacity; // power of 2

		Entry<K,V>[] oldTable = getTable();
		int oldCapacity = oldTable.length;

		// check if needed
		if ( size < threshold || oldCapacity > newCapacity ) {
			return;
		}

		Entry<K,V>[] newTable = new Entry[newCapacity];

		transfer( oldTable, newTable );
		table = newTable;

		/*
				 * If ignoring null elements and processing ref queue caused massive
				 * shrinkage, then restore old table.  This should be rare, but avoids
				 * unbounded expansion of garbage-filled tables.
				 */
		if ( size >= threshold / 2 ) {
			threshold = (int) ( newCapacity * loadFactor );
		}
		else {
			expungeStaleEntries();
			transfer( newTable, oldTable );
			table = oldTable;
		}
	}

	/**
	 * Transfer all entries from src to dest tables
	 */
	private void transfer(Entry<K,V>[] src, Entry<K,V>[] dest) {
		for ( int j = 0; j < src.length; ++j ) {
			Entry<K,V> e = src[j];
			src[j] = null;
			while ( e != null ) {
				Entry<K,V> next = e.next;
				K key = e.get();
				if ( key == null ) {
					// Assign null helps GC
					e.next = null;
					e.value = null;
					size--;
				}
				else {
					int i = indexFor( e.hash, dest.length );
					e.next = dest[i];
					dest[i] = e;
				}
				e = next;
			}
		}
	}

	/**
	 * Copies all of the mappings from the specified map to this map These
	 * mappings will replace any mappings that this map had for any of the
	 * keys currently in the specified map.<p>
	 *
	 * @param t mappings to be stored in this map.
	 * @throws NullPointerException if the specified map is null.
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> t) {
		// Expand enough to hold t's elements without resizing.
		int n = t.size();
		if ( n == 0 ) {
			return;
		}
		if ( n >= threshold ) {
			n = (int) ( n / loadFactor + 1 );
			if ( n > MAXIMUM_CAPACITY ) {
				n = MAXIMUM_CAPACITY;
			}
			int capacity = table.length;
			while ( capacity < n ) {
				capacity <<= 1;
			}
			resize( capacity );
		}

		for ( Iterator i = t.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<K,V> e = (Map.Entry<K,V>) i.next(); //FIXME should not have to cast
			put( e.getKey(), e.getValue() );
		}
	}

	/**
	 * Removes the mapping for this key from this map if present.
	 *
	 * @param key key whose mapping is to be removed from the map.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *         if there was no mapping for key.  A <tt>null</tt> return can
	 *         also indicate that the map previously associated <tt>null</tt>
	 *         with the specified key.
	 */
	@Override
	public V remove(Object key) {
		Object k = maskNull( key );
		int h = hash( k );
		Entry<K,V>[] tab = getTable();
		int i = indexFor( h, tab.length );
		Entry<K,V> prev = tab[i];
		Entry<K,V> e = prev;

		while ( e != null ) {
			Entry<K,V> next = e.next;
			if ( h == e.hash && k == e.get() ) {
				modCount++;
				size--;
				if ( prev == e ) {
					tab[i] = next;
				}
				else {
					prev.next = next;
				}
				return e.value;
			}
			prev = e;
			e = next;
		}

		return null;
	}


	/**
	 * Special version of remove needed by Entry set
	 */
	Entry removeMapping(Object o) {
		if ( !( o instanceof Map.Entry ) ) {
			return null;
		}
		Entry[] tab = getTable();
		Map.Entry entry = (Map.Entry) o;
		Object k = maskNull( entry.getKey() );
		int h = hash( k );
		int i = indexFor( h, tab.length );
		Entry prev = tab[i];
		Entry e = prev;

		while ( e != null ) {
			Entry next = e.next;
			if ( h == e.hash && e.equals( entry ) ) {
				modCount++;
				size--;
				if ( prev == e ) {
					tab[i] = next;
				}
				else {
					prev.next = next;
				}
				return e;
			}
			prev = e;
			e = next;
		}

		return null;
	}

	/**
	 * Removes all mappings from this map.
	 */
	@Override
	public void clear() {
		// clear out ref queue. We don't need to expunge entries
		// since table is getting cleared.
		while ( queue.poll() != null ) {
			//no-op
		}

		modCount++;
		Entry tab[] = table;
		for ( int i = 0; i < tab.length; ++i ) {
			tab[i] = null;
		}
		size = 0;

		// Allocation of array may have caused GC, which may have caused
		// additional entries to go stale.  Removing these entries from the
		// reference queue will make them eligible for reclamation.
		while ( queue.poll() != null ) {
			//no-op
		}
	}

	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the
	 * specified value.
	 *
	 * @param value value whose presence in this map is to be tested.
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 *         specified value.
	 */
	@Override
	public boolean containsValue(Object value) {
		if ( value == null ) {
			return containsNullValue();
		}

		Entry tab[] = getTable();
		for ( int i = tab.length; i-- > 0; ) {
			for ( Entry e = tab[i]; e != null; e = e.next ) {
				if ( value.equals( e.value ) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Special-case code for containsValue with null argument
	 */
	private boolean containsNullValue() {
		Entry tab[] = getTable();
		for ( int i = tab.length; i-- > 0; ) {
			for ( Entry e = tab[i]; e != null; e = e.next ) {
				if ( e.value == null ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Remove elements having the according value.
	 * Intended to avoid concurrent access exceptions
	 * It is expected that nobody add a key being removed by value
	 *
	 * @param value value whose presence in this map is to be removed.
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 *         specified value.
	 */
	public boolean removeValue(Object value) {
		if ( value == null ) {
			return removeNullValue();
		}

		Entry tab[] = getTable();
		Set keys = new HashSet();
		for ( int i = tab.length; i-- > 0; ) {
			for ( Entry e = tab[i]; e != null; e = e.next ) {
				if ( value.equals( e.value ) ) {
					keys.add( e.getKey() );
				}
			}
		}
		for ( Object key : keys ) {
			remove( key );
		}
		return !keys.isEmpty();
	}

	/**
	 * Special-case code for removeValue with null argument
	 */
	private boolean removeNullValue() {
		Entry tab[] = getTable();
		Set keys = new HashSet();
		for ( int i = tab.length; i-- > 0; ) {
			for ( Entry e = tab[i]; e != null; e = e.next ) {
				if ( e.value == null ) {
					keys.add( e.getKey() );
				}
			}
		}
		for ( Object key : keys ) {
			remove( key );
		}
		return !keys.isEmpty();
	}

	/**
	 * The entries in this hash table extend WeakReference, using its main ref
	 * field as the key.
	 */
	private static class Entry<K,V> extends WeakReference<K> implements Map.Entry<K,V> {
		private V value;
		private final int hash;
		private Entry<K,V> next;

		/**
		 * Create new entry.
		 */
		Entry(K key, V value, ReferenceQueue queue,
			int hash, Entry<K,V> next) {
			super( key, queue );
			this.value = value;
			this.hash = hash;
			this.next = next;
		}

		@Override
		public K getKey() {
			return WeakIdentityHashMap.unmaskNull( this.get() );
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}

		@Override
		public boolean equals(Object o) {
			if ( !( o instanceof Map.Entry ) ) {
				return false;
			}
			Map.Entry e = (Map.Entry) o;
			Object k1 = getKey();
			Object k2 = e.getKey();
			if ( k1 == k2 ) {
				Object v1 = getValue();
				Object v2 = e.getValue();
				if ( v1 == v2 || ( v1 != null && v1.equals( v2 ) ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			Object k = getKey();
			Object v = getValue();
			return ( ( k == null ?
					0 :
					System.identityHashCode( k ) ) ^
					( v == null ?
							0 :
							v.hashCode() ) );
		}

		@Override
		public String toString() {
			return getKey() + "=" + getValue();
		}
	}

	private abstract class HashIterator<E> implements Iterator<E> {
		int index;
		Entry<K,V> entry = null;
		Entry<K,V> lastReturned = null;
		int expectedModCount = modCount;

		/**
		 * Strong reference needed to avoid disappearance of key
		 * between hasNext and next
		 */
		Object nextKey = null;

		/**
		 * Strong reference needed to avoid disappearance of key
		 * between nextEntry() and any use of the entry
		 */
		Object currentKey = null;

		HashIterator() {
			index = ( size() != 0 ?
					table.length :
					0 );
		}

		@Override
		public boolean hasNext() {
			Entry[] t = table;

			while ( nextKey == null ) {
				Entry e = entry;
				int i = index;
				while ( e == null && i > 0 ) {
					e = t[--i];
				}
				entry = e;
				index = i;
				if ( e == null ) {
					currentKey = null;
					return false;
				}
				nextKey = e.get(); // hold on to key in strong ref
				if ( nextKey == null ) {
					entry = entry.next;
				}
			}
			return true;
		}

		/**
		 * The common parts of next() across different types of iterators
		 */
		protected Entry<K,V> nextEntry() {
			if ( modCount != expectedModCount ) {
				throw new ConcurrentModificationException();
			}
			if ( nextKey == null && !hasNext() ) {
				throw new NoSuchElementException();
			}

			lastReturned = entry;
			entry = entry.next;
			currentKey = nextKey;
			nextKey = null;
			return lastReturned;
		}

		@Override
		public void remove() {
			if ( lastReturned == null ) {
				throw new IllegalStateException();
			}
			if ( modCount != expectedModCount ) {
				throw new ConcurrentModificationException();
			}

			WeakIdentityHashMap.this.remove( currentKey );
			expectedModCount = modCount;
			lastReturned = null;
			currentKey = null;
		}

	}

	private class ValueIterator extends HashIterator {
		@Override
		public Object next() {
			return nextEntry().value;
		}
	}

	private class KeyIterator extends HashIterator {
		@Override
		public Object next() {
			return nextEntry().getKey();
		}
	}

	private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
		@Override
		public Map.Entry<K,V> next() {
			return nextEntry();
		}
	}

	/**
	 * Returns a set view of the keys contained in this map.  The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa.  The set supports element removal, which removes the
	 * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 *
	 * @return a set view of the keys contained in this map.
	 */
	@Override
	public Set keySet() {
		Set ks = keySet;
		return ( ks != null ?
				ks :
				( keySet = new KeySet() ) );
	}

	private class KeySet extends AbstractSet {
		@Override
		public Iterator iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey( o );
		}

		@Override
		public boolean remove(Object o) {
			if ( containsKey( o ) ) {
				WeakIdentityHashMap.this.remove( o );
				return true;
			}
			else {
				return false;
			}
		}

		@Override
		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		@Override
		public Object[] toArray() {
			Collection c = new ArrayList( size() );
			for ( Iterator i = iterator(); i.hasNext(); ) {
				c.add( i.next() );
			}
			return c.toArray();
		}

		@Override
		public Object[] toArray(Object a[]) {
			Collection c = new ArrayList( size() );
			for ( Iterator i = iterator(); i.hasNext(); ) {
				c.add( i.next() );
			}
			return c.toArray( a );
		}
	}

	/**
	 * Returns a collection view of the values contained in this map.  The
	 * collection is backed by the map, so changes to the map are reflected in
	 * the collection, and vice-versa.  The collection supports element
	 * removal, which removes the corresponding mapping from this map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
	 * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the values contained in this map.
	 */
	@Override
	public Collection values() {
		Collection vs = values;
		return ( vs != null ?
				vs :
				( values = new Values() ) );
	}

	private class Values extends AbstractCollection {
		public Iterator iterator() {
			return new ValueIterator();
		}

		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		public boolean contains(Object o) {
			return containsValue( o );
		}

		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		public Object[] toArray() {
			Collection c = new ArrayList( size() );
			for ( Iterator i = iterator(); i.hasNext(); ) {
				c.add( i.next() );
			}
			return c.toArray();
		}

		public Object[] toArray(Object a[]) {
			Collection c = new ArrayList( size() );
			for ( Iterator i = iterator(); i.hasNext(); ) {
				c.add( i.next() );
			}
			return c.toArray( a );
		}
	}

	/**
	 * Returns a collection view of the mappings contained in this map.  Each
	 * element in the returned collection is a <tt>Map.Entry</tt>.  The
	 * collection is backed by the map, so changes to the map are reflected in
	 * the collection, and vice-versa.  The collection supports element
	 * removal, which removes the corresponding mapping from the map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
	 * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the mappings contained in this map.
	 * @see java.util.Map.Entry
	 */
	public Set<Map.Entry<K,V>> entrySet() {
		Set<Map.Entry<K,V>> es = entrySet;
		return ( es != null ?
				es :
				( entrySet = new EntrySet() ) );
	}

	private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
		public Iterator<Map.Entry<K,V>> iterator() {
			return new EntryIterator();
		}

		public boolean contains(Object o) {
			if ( !( o instanceof Map.Entry ) ) {
				return false;
			}
			Map.Entry e = (Map.Entry) o;
			Object k = e.getKey();
			Entry candidate = getEntry( k );
			return candidate != null && candidate.equals( e );
		}

		public boolean remove(Object o) {
			return removeMapping( o ) != null;
		}

		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		public Object[] toArray() {
			Collection c = new ArrayList( size() );
			for ( Iterator i = iterator(); i.hasNext(); ) {
				c.add( new SimpleEntry( (Map.Entry) i.next() ) );
			}
			return c.toArray();
		}

		public Object[] toArray(Object a[]) {
			Collection c = new ArrayList( size() );
			for ( Iterator i = iterator(); i.hasNext(); ) {
				c.add( new SimpleEntry( (Map.Entry) i.next() ) );
			}
			return c.toArray( a );
		}
	}

	static class SimpleEntry implements Map.Entry {
		Object key;
		Object value;

		public SimpleEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}

		public SimpleEntry(Map.Entry e) {
			this.key = e.getKey();
			this.value = e.getValue();
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}

		public Object setValue(Object value) {
			Object oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean equals(Object o) {
			if ( !( o instanceof Map.Entry ) ) {
				return false;
			}
			Map.Entry e = (Map.Entry) o;
			return eq( key, e.getKey() ) && eq( value, e.getValue() );
		}

		public int hashCode() {
			return ( ( key == null ) ?
					0 :
					key.hashCode() ) ^
					( ( value == null ) ?
							0 :
							value.hashCode() );
		}

		public String toString() {
			return key + "=" + value;
		}

		private static boolean eq(Object o1, Object o2) {
			return ( o1 == null ?
					o2 == null :
					o1.equals( o2 ) );
		}
	}

}
