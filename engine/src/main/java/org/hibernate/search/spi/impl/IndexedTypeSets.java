/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;

public final class IndexedTypeSets {

	private IndexedTypeSets() {
		//Utility class: not to be constructed
	}

	public static IndexedTypeSet fromClasses(Class<?>... classes) {
		if ( classes == null || classes.length == 0 ) {
			// "null" needs to be acceptable to support some legacy use cases
			return empty();
		}
		else {
			return Arrays.stream( classes ).filter( c -> c != null ).map( PojoIndexedTypeIdentifier::new ).collect( streamCollector() );
		}
	}

	public static IndexedTypeSet fromClass(Class<?> clazz) {
		Objects.requireNonNull( clazz );
		return new PojoIndexedTypeIdentifier( clazz ).asTypeSet();
	}

	public static IndexedTypeSet empty() {
		return HashSetIndexedTypeSet.EMPTY;
	}

	public static IndexedTypeSet fromIdentifiers(Iterable<IndexedTypeIdentifier> entityTypes) {
		Objects.requireNonNull( entityTypes );
		if ( entityTypes instanceof Set<?> ) {
			Set<IndexedTypeIdentifier> set = (Set<IndexedTypeIdentifier>) entityTypes;
			fromSafeHashSet( new HashSet<IndexedTypeIdentifier>( set ) );
		}
		HashSet<IndexedTypeIdentifier> set = new HashSet<>();
		for ( IndexedTypeIdentifier iti : entityTypes ) {
			set.add( iti );
		}
		return fromSafeHashSet( set );
	}

	public static IndexedTypeSet fromIdentifiers(IndexedTypeIdentifier... types) {
		if ( types == null || types.length == 0 ) {
			// "null" needs to be acceptable to support some legacy use cases
			return empty();
		}
		else if ( types.length == 1 ) {
			return types[0].asTypeSet();
		}
		else {
			return Arrays.stream( types ).collect( streamCollector() );
		}
	}

	private static IndexedTypeSet fromSafeHashSet(HashSet<IndexedTypeIdentifier> set) {
		if ( set.isEmpty() ) {
			return empty();
		}
		else if ( set.size() == 1 ) {
			return set.iterator().next().asTypeSet();
		}
		else {
			return new HashSetIndexedTypeSet( set );
		}
	}

	public static IndexedTypeSet composite(final IndexedTypeSet set, final IndexedTypeIdentifier additionalId) {
		return composite( set, additionalId.asTypeSet() );
	}

	public static IndexedTypeSet composite(final IndexedTypeSet setA, final IndexedTypeSet setB) {
		if ( setA.isEmpty() ) {
			return setB;
		}
		else if ( setB.isEmpty() ) {
			return setA;
		}
		else if ( setB.equals( setA ) ) {
			return setA;
		}
		else {
			HashSet<IndexedTypeIdentifier> newSet = new HashSet<>( setA.size() + setB.size() );
			setA.forEach( newSet::add );
			setB.forEach( newSet::add );
			return fromSafeHashSet( newSet );
		}
	}

	public static IndexedTypeSet subtraction(IndexedTypeSet referenceSet, IndexedTypeSet subtraend) {
		if ( referenceSet.isEmpty() || subtraend.isEmpty() ) {
			return referenceSet;
		}
		else {
			HashSetIndexedTypeSet casted = (HashSetIndexedTypeSet) referenceSet;
			Set<IndexedTypeIdentifier> cloned = casted.cloneInternalSet();
			for ( IndexedTypeIdentifier toRemove : subtraend ) {
				cloned.remove( toRemove );
			}
			if ( cloned.isEmpty() ) {
				return empty();
			}
			if ( cloned.size() == 1 ) {
				return cloned.iterator().next().asTypeSet();
			}
			else {
				return new HashSetIndexedTypeSet( cloned );
			}
		}
	}

	private static final Set<Collector.Characteristics> CH_UNORDERED = Collections.unmodifiableSet( EnumSet.of( Collector.Characteristics.UNORDERED ) );

	public static Collector<IndexedTypeIdentifier,?,IndexedTypeSet> streamCollector() {
		return new Collector<IndexedTypeIdentifier,HashSet<IndexedTypeIdentifier>,IndexedTypeSet>() {

			@Override
			public Supplier<HashSet<IndexedTypeIdentifier>> supplier() {
				return HashSet::new;
			}

			@Override
			public BiConsumer<HashSet<IndexedTypeIdentifier>, IndexedTypeIdentifier> accumulator() {
				return ( s, i ) -> s.add( i );
			}

			@Override
			public BinaryOperator<HashSet<IndexedTypeIdentifier>> combiner() {
				return ( a, b ) -> {
						a.addAll( b );
						return a;
					};
			}

			@Override
			public Function<HashSet<IndexedTypeIdentifier>, IndexedTypeSet> finisher() {
				return IndexedTypeSets::fromSafeHashSet;
			}

			@Override
			public Set<java.util.stream.Collector.Characteristics> characteristics() {
				return CH_UNORDERED;
			}

		};
	}

}
