/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Comparator;

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
}
