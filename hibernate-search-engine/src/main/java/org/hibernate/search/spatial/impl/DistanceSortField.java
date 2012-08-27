package org.hibernate.search.spatial.impl;

import org.apache.lucene.search.SortField;

/**
 * Lucene SortField for sorting documents which have been indexed with Hibernate Search spatial
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByGrid
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByRange
 * @see org.hibernate.search.spatial.Coordinates
 */
public class DistanceSortField extends SortField {

	public DistanceSortField(Point center, String fieldname) {
		super(fieldname, new DistanceComparatorSource( center ));
	}

	public DistanceSortField(double latitude, double longitude, String fieldname) {
		this( Point.fromDegrees( latitude, longitude ), fieldname );
	}
}
