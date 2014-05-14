/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldCache.Doubles;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredDocIdSet;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.util.Bits;
import org.hibernate.search.spatial.Coordinates;

/**
 * Lucene Filter for filtering documents which have been indexed with Hibernate Search spatial Field bridge
 * Use double lat,long field in the index from a Coordinates field declaration
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByHash
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByRange
 * @see org.hibernate.search.spatial.Coordinates
 */
public final class DistanceFilter extends Filter {

	private Filter previousFilter;
	private Point center;
	private double radius;
	private String coordinatesField;
	private String latitudeField;
	private String longitudeField;

	/**
	 * Construct a Distance Filter to match document distant at most of radius from center Point
	 *
	 * @param previousFilter previous Filter in the chain. As Distance is costly by retrieving the lat and long field
	 * it is better to use it last
	 * @param centerCoordinates center of the search perimeter
	 * @param radius radius of the search perimeter
	 * @param coordinatesField name of the field implementing Coordinates
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public DistanceFilter(Filter previousFilter, Coordinates centerCoordinates, double radius, String coordinatesField) {
		if ( previousFilter != null ) {
			this.previousFilter = previousFilter;
		}
		else {
			this.previousFilter = new QueryWrapperFilter( new MatchAllDocsQuery() );
		}
		this.center = Point.fromCoordinates( centerCoordinates );
		this.radius = radius;
		this.coordinatesField = coordinatesField;
	}

	/**
	 * Construct a Distance Filter to match document distant at most of radius from center Point
	 *
	 * @param previousFilter previous Filter in the chain. As Distance is costly by retrieving the lat and long field
	 * it is better to use it last
	 * @param centerCoordinates center of the search perimeter
	 * @param radius radius of the search perimeter
	 * @param latitudeField name of the field hosting latitude
	 * @param longitudeField name of the field hosting longitude
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public DistanceFilter(Filter previousFilter, Coordinates centerCoordinates, double radius, String latitudeField, String longitudeField) {
		if ( previousFilter != null ) {
			this.previousFilter = previousFilter;
		}
		else {
			this.previousFilter = new QueryWrapperFilter( new MatchAllDocsQuery() );
		}
		this.center = Point.fromCoordinates( centerCoordinates );
		this.radius = radius;
		this.coordinatesField = null;
		this.latitudeField = latitudeField;
		this.longitudeField = longitudeField;
	}

	/**
	 * Returns Doc Ids by retrieving their lat,long and checking if within distance(radius) of the center of the search
	 *
	 * @param reader reader to the index
	 */
	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		final AtomicReader atomicReader = context.reader();
		final Doubles latitudeValues = FieldCache.DEFAULT.getDoubles( atomicReader, getLatitudeField(), false );
		final Doubles longitudeValues = FieldCache.DEFAULT.getDoubles( atomicReader, getLongitudeField(), false );

		DocIdSet docs = previousFilter.getDocIdSet( context, acceptDocs );

		if ( ( docs == null ) || ( docs.iterator() == null ) ) {
			return null;
		}

		return new FilteredDocIdSet( docs ) {
			@Override
			protected boolean match(int documentIndex) {

				if ( center.getDistanceTo( latitudeValues.get( documentIndex ), longitudeValues.get( documentIndex ) ) <= radius ) {
					return true;
				}
				else {
					return false;
				}
			}
		};
	}

	private String getLatitudeField() {
		if ( latitudeField != null ) {
			return latitudeField;
		}
		else {
			return SpatialHelper.formatLatitude( coordinatesField );
		}
	}

	private String getLongitudeField() {
		if ( longitudeField != null ) {
			return longitudeField;
		}
		else {
			return SpatialHelper.formatLongitude( coordinatesField );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "DistanceFilter" );
		sb.append( "{previousFilter=" ).append( previousFilter );
		sb.append( ", center=" ).append( center );
		sb.append( ", radius=" ).append( radius );
		if ( coordinatesField != null ) {
			sb.append( ", coordinatesField='" ).append( coordinatesField ).append( '\'' );
		}
		else {
			sb.append( ", latitudeField=" ).append( latitudeField );
			sb.append( ", longitudeField=" ).append( longitudeField ).append( '\'' );
		}
		sb.append( '}' );
		return sb.toString();
	}
}
