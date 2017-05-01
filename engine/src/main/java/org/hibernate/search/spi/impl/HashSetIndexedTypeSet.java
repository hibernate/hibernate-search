/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;

final class HashSetIndexedTypeSet implements IndexedTypeSet, Serializable {

	public static final IndexedTypeSet EMPTY = new HashSetIndexedTypeSet( Collections.EMPTY_SET );

	private final Set<IndexedTypeIdentifier> set;

	HashSetIndexedTypeSet(IndexedTypeIdentifier singleton) {
		Objects.requireNonNull( singleton );
		this.set = Collections.singleton( singleton );
	}

	HashSetIndexedTypeSet(Set<IndexedTypeIdentifier> set) {
		Objects.requireNonNull( set );
		if ( set.size() == 1 ) {
			throw new AssertionFailure( "singleton sets should not be constructed using this implementation" );
		}
		this.set = set;
	}

	@Override
	public Iterator<IndexedTypeIdentifier> iterator() {
		return set.iterator();
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	@Deprecated
	public Set<Class<?>> toPojosSet() {
		return set.stream().map( IndexedTypeIdentifier::getPojoType ).collect( Collectors.toSet() );
	}

	boolean contains(IndexedTypeIdentifier id) {
		return set.contains( id );
	}

	Set<IndexedTypeIdentifier> cloneInternalSet() {
		return new HashSet( set );
	}

	@Override
	public String toString() {
		return set.toString();
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		else if ( obj == null ) {
			return false;
		}
		else if ( HashSetIndexedTypeSet.class != obj.getClass() ) {
				return false;
		}
		else {
			HashSetIndexedTypeSet other = (HashSetIndexedTypeSet) obj;
			return set.equals( other.set );
		}
	}

	@Override
	public boolean containsAll(IndexedTypeSet subsetCandidate) {
		for ( IndexedTypeIdentifier e : subsetCandidate ) {
			if ( ! set.contains( e ) ) {
				return false;
			}
		}
		return true;
	}

}
