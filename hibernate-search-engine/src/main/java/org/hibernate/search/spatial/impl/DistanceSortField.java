package org.hibernate.search.spatial.impl;

import org.apache.lucene.search.SortField;

/**
 * Created with IntelliJ IDEA.
 * User: nicolashelleringer
 * Date: 26/07/12
 * Time: 14:36
 * To change this template use File | Settings | File Templates.
 */
public class DistanceSortField extends SortField {

	public DistanceSortField(Point center) {
		super("Distance", new DistanceComparatorSource( center ));
	}

	public DistanceSortField(double latitude, double longitude) {
		this( Point.fromDegrees( latitude, longitude ) );
	}
}
