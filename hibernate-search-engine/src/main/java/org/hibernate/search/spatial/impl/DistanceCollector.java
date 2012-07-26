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
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.Scorer;

public class DistanceCollector extends Collector {

	private Collector delegate;
	private boolean acceptsDocsOutOfOrder;

	private Point center;
	private String latitudeField;
	private String longitudeField;
	private int docBase = 0;
	private double[] distances;
	private double[] latitudeValues;
	private double[] longitudeValues;

	public DistanceCollector(Collector delegate, Point center, int hitsCount, String fieldname) {
		this.delegate= delegate;
		this.acceptsDocsOutOfOrder = delegate.acceptsDocsOutOfOrder();
		this.center = center;
		this.distances = new double[hitsCount];
		this.latitudeValues = new double[hitsCount];
		this.longitudeValues = new double[hitsCount];
		this.latitudeField = GridHelper.formatLatitude( fieldname );
		this.longitudeField = GridHelper.formatLongitude(  fieldname );
	}

	@Override
	public void setScorer(final Scorer scorer) throws IOException {
		delegate.setScorer( scorer );
		return;
	}

	@Override
	public void collect(final int doc) throws IOException {
		delegate.collect( doc );
		final int absolute = docBase + doc;
		distances[absolute] = center.getDistanceTo( latitudeValues[absolute], longitudeValues[absolute] );
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		delegate.setNextReader( reader, docBase );
		double[] unbasedLatitudeValues = FieldCache.DEFAULT.getDoubles( reader, latitudeField );
		double[] unbasedLongitudeValues = FieldCache.DEFAULT.getDoubles( reader, longitudeField );
		this.docBase = docBase;
		for ( int i = 0 ; i < unbasedLatitudeValues.length ; i ++ ) {
			latitudeValues[this.docBase + i] = unbasedLatitudeValues[i];
			longitudeValues[this.docBase + i] = unbasedLongitudeValues[i];
		}
		return;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return acceptsDocsOutOfOrder;
	}

	public double getDistance(final int index) {
		return distances[index];
	}
}
