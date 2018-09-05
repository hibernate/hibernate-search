/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.datasets;


class DatasetUtils {

	private DatasetUtils() {
	}

	/**
	 * Convert an integer to an arbitrary float.
	 * <p>
	 * The float is guaranteed not to be NaN nor infinite,
	 * which can come handy with Elasticsearch (which doesn't accept those by default).
	 *
	 * @param integer The integer to convert
	 * @return A float
	 */
	public static float intToFloat(int integer) {
		float result = Float.intBitsToFloat( integer );
		if ( Float.isNaN( result ) ) {
			/*
			 * See Float.intBitsToFloat:
			 * NaN are generated for integers in the range
			 * {@code 0x7f800001} through {@code 0x7fffffff} or in
			 * the range {@code 0xff800001} through
			 * {@code 0xffffffff}.
			 * Positive infinite is generated for 0x7f800000.
			 * Negative infinite is generated for 0xff800000.
			 * Thus, removing 0x00800001 is enough to ensure the generated float
			 * will not be infinite nor NaN.
			 */
			result = Float.intBitsToFloat( integer - 0x00800001 );
		}
		return result;
	}
}
