/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

public class TestComparators {

	public static final Comparator<Double> APPROX_M_COMPARATOR = approximateDouble( 10.0 );
	public static final Comparator<Double> APPROX_KM_COMPARATOR = approximateDouble( 0.010 );
	public static final Comparator<Double> APPROX_MILES_COMPARATOR = approximateDouble( 0.006 );

	private TestComparators() {
	}

	/**
	 * A {@link Comparator} that considers two doubles are equal
	 * when they are separated by less than a given delta.
	 */
	public static Comparator<Double> approximateDouble(double delta) {
		return Comparator.nullsFirst( (a, b) -> {
			if ( a < b - delta ) {
				return 1;
			}
			if ( b + delta < a ) {
				return -1;
			}
			return 0;
		} );
	}

	/**
	 * Creates a {@link Comparator} that produces a lexicographic order based on a given element comparator.
	 * <p>
	 * If a list is a prefix of the other, the shortest list is deemed "lower".
	 */
	public static <T> Comparator<Iterable<T>> lexicographic(Comparator<T> elementComparator) {
		return Comparator.nullsFirst( (a, b) -> {
			Iterator<T> aIt = a.iterator();
			Iterator<T> bIt = b.iterator();
			while ( aIt.hasNext() && bIt.hasNext() ) {
				T aElem = aIt.next();
				T bElem = bIt.next();
				int elemOrder = elementComparator.compare( aElem, bElem );
				if ( elemOrder != 0 ) {
					return elemOrder; // lexicographical ordering: the first differing element dictates the order.
				}
			}
			if ( aIt.hasNext() ) {
				return -1; // b is a prefix of a
			}
			else if ( bIt.hasNext() ) {
				return 1; // a is a prefix of b
			}
			else {
				return 0; // a and b have the same number of elements, all equal according to the element comparator
			}
		} );
	}

	public static <T> Comparator<Optional<T>> optional(Comparator<T> elementComparator) {
		return Comparator.nullsFirst( (a, b) -> {
			if ( a.isPresent() && b.isPresent() ) {
				return elementComparator.compare( a.get(), b.get() );
			}
			if ( a.isEmpty() && b.isEmpty() ) {
				return 0;
			}
			return a.isEmpty() ? -1 : 1;
		} );
	}

	public static <T> Comparator<T[]> array(Comparator<T> elementComparator) {
		return Comparator.nullsFirst( (a, b) -> {
			int index = 0;
			int min = Math.min( a.length, b.length );
			for ( ; index < min; index++ ) {
				T aElem = a[index];
				T bElem = b[index];
				int elemOrder = elementComparator.compare( aElem, bElem );
				if ( elemOrder != 0 ) {
					return elemOrder; // lexicographical ordering: the first differing element dictates the order.
				}
			}
			if ( a.length < b.length ) {
				return -1; // b is a prefix of a
			}
			else if ( a.length > b.length ) {
				return 1; // a is a prefix of b
			}
			else {
				return 0; // a and b have the same number of elements, all equal according to the element comparator
			}
		} );
	}
}
