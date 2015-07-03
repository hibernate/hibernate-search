/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import org.apache.lucene.index.NumericDocValues;

/**
 * Utility class containing functions to read the value of a spatial coordinate field from an index and convert it to a
 * double.
 *
 * @author Davide D'Alto
 */
public final class CoordinateHelper {

	private CoordinateHelper() {
	}

	public static double coordinate(NumericDocValues docValues, int docID) {
		return Double.longBitsToDouble( docValues.get( docID ) );
	}
}
