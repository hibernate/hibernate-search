/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial;

import org.apache.lucene.search.SortField;

import org.hibernate.search.spatial.impl.DistanceComparatorSource;
import org.hibernate.search.spatial.impl.Point;

/**
 * Lucene SortField for sorting documents which have been indexed with Hibernate Search spatial
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByHash
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByRange
 * @see org.hibernate.search.spatial.Coordinates
 */
public class DistanceSortField extends SortField {

	public DistanceSortField(Coordinates center, String fieldName) {
		super( fieldName, new DistanceComparatorSource( center ) );
	}

	public DistanceSortField(double latitude, double longitude, String fieldName) {
		this( Point.fromDegrees( latitude, longitude ), fieldName );
	}
}
