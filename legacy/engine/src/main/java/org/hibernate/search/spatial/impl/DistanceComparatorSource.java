/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import java.io.IOException;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

import org.hibernate.search.spatial.Coordinates;

public class DistanceComparatorSource extends FieldComparatorSource {

	private final Point center;

	public DistanceComparatorSource(Coordinates center) {
		this.center = Point.fromCoordinates( center );
	}

	@Override
	public FieldComparator<?> newComparator(String fieldName, int numHits, int sortPos, boolean reversed)
			throws IOException {
		return new DistanceComparator( center, numHits, fieldName );
	}
}
