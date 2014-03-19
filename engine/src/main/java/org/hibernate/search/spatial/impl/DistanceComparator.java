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

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldCache.Doubles;
import org.apache.lucene.search.FieldComparator;

public final class DistanceComparator extends FieldComparator<Double> {

	private final Point center;
	private final String latitudeField;
	private final String longitudeField;
	private double[] distances;
	private Doubles latitudeValues;
	private Doubles longitudeValues;
	private Double bottomDistance;
	private Double topValue;

	public DistanceComparator(Point center, int numHits, String fieldName) {
		this.center = center;
		this.distances = new double[numHits];
		this.latitudeField = SpatialHelper.formatLatitude( fieldName );
		this.longitudeField = SpatialHelper.formatLongitude( fieldName );
	}

	@Override
	public int compare(final int slot1, final int slot2) {
		return Double.compare( distances[slot1], distances[slot2] );
	}

	@Override
	public void setBottom(final int slot) {
		bottomDistance = distances[slot];
	}

	@Override
	public void setTopValue(Double value) {
		topValue = value;
	}

	@Override
	public int compareBottom(final int doc) throws IOException {
		return Double.compare(
				bottomDistance,
				center.getDistanceTo( latitudeValues.get( doc ), longitudeValues.get( doc ) )
		);
	}

	@Override
	public int compareTop(int doc) throws IOException {
		if ( topValue == null ) {
			return 1; //we consider any doc "higher" than null
		}

		final double distanceTo = center.getDistanceTo( latitudeValues.get( doc ), longitudeValues.get( doc ) );
		return Double.compare( distanceTo, topValue );
	}

	@Override
	public void copy(final int slot, final int doc) throws IOException {
		distances[slot] = center.getDistanceTo(
				latitudeValues.get( doc ),
				longitudeValues.get( doc )
		);
	}

	@Override
	public DistanceComparator setNextReader(final AtomicReaderContext context) throws IOException {
		latitudeValues = FieldCache.DEFAULT.getDoubles( context.reader(), latitudeField, false );
		longitudeValues = FieldCache.DEFAULT.getDoubles( context.reader(), longitudeField, false );
		return this;
	}

	@Override
	public Double value(final int slot) {
		return distances[slot];
	}
}
