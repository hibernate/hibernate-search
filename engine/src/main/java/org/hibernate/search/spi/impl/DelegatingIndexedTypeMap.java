/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

// Must be serializable to support query serialization
final class DelegatingIndexedTypeMap<V> implements IndexedTypeMap<V>, Serializable {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final IndexedTypeMap EMPTY = new DelegatingIndexedTypeMap( Collections.EMPTY_MAP, Collections.EMPTY_MAP );

	private static final Log log = LoggerFactory.make();
	private static final IndexedTypeIdentifier ROOT_OBJECT = new PojoIndexedTypeIdentifier( Object.class );

	private final Map<IndexedTypeIdentifier,V> map;
	private final Map<String,IndexedTypeIdentifier> name2keyMapping;

	public DelegatingIndexedTypeMap(Map<IndexedTypeIdentifier, V> map,
			Map<String, IndexedTypeIdentifier> name2keyMapping) {
		super();
		this.map = map;
		this.name2keyMapping = name2keyMapping;
	}

	@Override
	public V get(IndexedTypeIdentifier key) {
		return map.get( key );
	}

	@Override
	public Iterable<Entry<IndexedTypeIdentifier, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public V get(Class<?> legacyPojo) {
		return map.get( new PojoIndexedTypeIdentifier( legacyPojo ) );
	}

	@Override
	public IndexedTypeSet keySet() {
		return IndexedTypeSets.fromIdentifiers( map.keySet() );
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public boolean containsKey(IndexedTypeIdentifier type) {
		return map.containsKey( type );
	}

	@Override
	public void put(IndexedTypeIdentifier type, V value) {
		//technically there's a race condition here but we only do
		//writes during initialization, which is sequential.
		//Writing the names first makes this safe even for parallel
		//initializations.
		name2keyMapping.put( type.getName(), type );
		map.put( type, value );
	}

	@Override
	public IndexedTypeIdentifier keyFromName(String entityClassName) {
		if ( entityClassName == null ) {
			throw log.nullIsInvalidIndexedType();
		}
		IndexedTypeIdentifier id = name2keyMapping.get( entityClassName );
		if ( id == null ) {
			throw log.notAnIndexedType( entityClassName );
		}
		return id;
	}

	@Override
	public IndexedTypeIdentifier keyFromPojoType(Class<?> clazz) {
		if ( clazz == null ) {
			throw log.nullIsInvalidIndexedType();
		}
		if ( clazz == Object.class ) {
			// Odd case: to handle existing semantics we need to be able to
			// identify the root Object even though we consider it's not included
			// in the maps.
			return ROOT_OBJECT;
		}
		return new PojoIndexedTypeIdentifier( clazz );
	}

}
