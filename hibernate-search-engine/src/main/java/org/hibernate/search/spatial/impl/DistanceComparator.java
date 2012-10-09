/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

public class DistanceComparator extends FieldComparator<Double> {

	private Point center;
	private String latitudeField;
	private String longitudeField;
	private Double bottomDistance;
	private int docBase = 0;
	private double[] distances;
	private double[] latitudeValues;
	private double[] longitudeValues;

	public DistanceComparator(Point center, int hitsCount, String fieldname) {
		this.center = center;
		this.distances = new double[ hitsCount ];
		this.latitudeValues = new double[ hitsCount ];
		this.longitudeValues = new double[ hitsCount ];
		this.latitudeField = GridHelper.formatLatitude( fieldname );
		this.longitudeField = GridHelper.formatLongitude(  fieldname );
	}

	@Override
	public int compare(int slot1, int slot2) {
		return Double.compare( distances[ slot1 ], distances [ slot2 ]);
	}

	@Override
	public void setBottom(int slot) {
		bottomDistance = center.getDistanceTo( latitudeValues[slot], longitudeValues[slot] );
	}

	@Override
	public int compareBottom(int doc) throws IOException {
		return Double.compare( bottomDistance, distances[ doc ]);
	}

	@Override
	public void copy(int slot, int doc) throws IOException {
		distances[ slot ] = center.getDistanceTo( latitudeValues[doc], longitudeValues[doc] );
		return;
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		double[] unbasedLatitudeValues = FieldCache.DEFAULT.getDoubles( reader, latitudeField );
		double[] unbasedLongitudeValues = FieldCache.DEFAULT.getDoubles( reader, longitudeField );
		this.docBase = docBase;
		for ( int i = 0 ; i < unbasedLatitudeValues.length ; i ++ ) {
			latitudeValues[ this.docBase + i ] = unbasedLatitudeValues[ i ];
			longitudeValues[ this.docBase + i ] = unbasedLongitudeValues[ i ];
		}
		return;
	}

	@Override
	public Double value(int slot) {
		return center.getDistanceTo( latitudeValues[slot], longitudeValues[slot] );
	}
}
