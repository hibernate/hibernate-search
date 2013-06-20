/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.spatial.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.util.collections.IntToDoubleMap;

public final class DistanceComparator extends FieldComparator<Double> {

	private final Point center;
	private final String latitudeField;
	private final String longitudeField;
	private IntToDoubleMap distances;
	private IntToDoubleMap latitudeValues;
	private IntToDoubleMap longitudeValues;

	private Double bottomDistance;
	private int docBase = 0;

	public DistanceComparator(Point center, int hitsCount, String fieldname) {
		this.center = center;
		this.distances = new IntToDoubleMap( hitsCount );
		this.latitudeValues = new IntToDoubleMap( hitsCount );
		this.longitudeValues = new IntToDoubleMap( hitsCount );
		this.latitudeField = SpatialHelper.formatLatitude( fieldname );
		this.longitudeField = SpatialHelper.formatLongitude( fieldname );
	}

	@Override
	public int compare(final int slot1, final int slot2) {
		return Double.compare( distances.get( slot1 ), distances.get( slot2 ) );
	}

	@Override
	public void setBottom(final int slot) {
		bottomDistance = distances.get( slot );
	}

	@Override
	public int compareBottom(final int doc) throws IOException {
		return Double.compare( bottomDistance, center.getDistanceTo( latitudeValues.get( docBase + doc ), longitudeValues.get( docBase + doc ) ) );
	}

	@Override
	public void copy(final int slot, final int doc) throws IOException {
		distances.put( slot, center.getDistanceTo( latitudeValues.get( docBase + doc ), longitudeValues.get( docBase + doc ) ) );
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		double[] unbasedLatitudeValues = FieldCache.DEFAULT.getDoubles( reader, latitudeField );
		double[] unbasedLongitudeValues = FieldCache.DEFAULT.getDoubles( reader, longitudeField );
		this.docBase = docBase;
		for ( int i = 0; i < unbasedLatitudeValues.length; i++ ) {
			latitudeValues.put( this.docBase + i, unbasedLatitudeValues[i] );
			longitudeValues.put( this.docBase + i, unbasedLongitudeValues[i] );
		}
	}

	@Override
	public Double value(final int slot) {
		return center.getDistanceTo( latitudeValues.get( slot ), longitudeValues.get( slot ) );
	}
}
