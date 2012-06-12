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
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredDocIdSet;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.QueryWrapperFilter;

/**
 * Lucene Filter for filtering documents which have been indexed with Hibernate Search Spatial SpatialFieldBridge
 * Use double lat,long field ine the index from a Coordinates field declaration
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @see org.hibernate.search.spatial.SpatialFieldBridge
 * @see org.hibernate.search.spatial.Coordinates
 */
public final class DistanceFilter extends Filter {

	private Filter previousFilter;
	private Point center;
	private double radius;
	private String fieldName;

	/**
	 * Construct a Distance Filter to match document distant at most of radius from center Point
	 *
	 * @param previousFilter previous Filter in the chain. As Distance is costly by retrieving the lat and long field
	 * it is better to use it last
	 * @param center center of the search perimeter
	 * @param radius radius of the search perimeter
	 * @param fieldName name of the field implementing Coordinates
	 *
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public DistanceFilter(Filter previousFilter, Point center, double radius, String fieldName) {
		if ( previousFilter != null ) {
			this.previousFilter = previousFilter;
		}
		else {
			this.previousFilter = new QueryWrapperFilter( new MatchAllDocsQuery() );
		}
		this.center = center;
		this.radius = radius;
		this.fieldName = fieldName;
	}

	/**
	 * Returns Doc Ids by retrieving their lat,long and checking if within distance(radius) of the center of the search
	 *
	 * @param reader reader to the index
	 */
	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {

		final double[] latitudeValues = FieldCache.DEFAULT.getDoubles( reader, GridHelper.formatLatitude( fieldName ) );
		final double[] longitudeValues = FieldCache.DEFAULT
				.getDoubles( reader, GridHelper.formatLongitude( fieldName ) );

		DocIdSet docs = previousFilter.getDocIdSet( reader );

		if ( ( docs == null ) || ( docs.iterator() == null ) ) {
			return null;
		}

		return new FilteredDocIdSet( docs ) {
			@Override
			protected boolean match(int documentIndex) {

				Double documentDistance;
				Point documentPosition = Point.fromDegrees(
						latitudeValues[documentIndex],
						longitudeValues[documentIndex]
				);
				documentDistance = documentPosition.getDistanceTo( center );
				if ( documentDistance <= radius ) {
					return true;
				}
				else {
					return false;
				}
			}
		};
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "DistanceFilter" );
		sb.append( "{previousFilter=" ).append( previousFilter );
		sb.append( ", center=" ).append( center );
		sb.append( ", radius=" ).append( radius );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}