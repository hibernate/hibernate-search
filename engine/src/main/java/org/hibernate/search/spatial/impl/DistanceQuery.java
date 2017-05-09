/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import static org.hibernate.search.spatial.impl.CoordinateHelper.coordinate;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.SpatialFieldBridgeByRange;

/**
 * Lucene distance Query for documents which have been indexed with {@link SpatialFieldBridgeByRange}
 * Use double lat,long field in the index from a Coordinates field declaration
 *
 * @author Nicolas Helleringer
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByHash
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByRange
 * @see org.hibernate.search.spatial.Coordinates
 */
public final class DistanceQuery extends Query {

	private final Query approximationQuery;
	private final Point center;
	private final double radius;
	private final String coordinatesField;
	private final String latitudeField;
	private final String longitudeField;

	/**
	 * Construct a distance query to match document distant at most of radius from center Point
	 *
	 * @param approximationQuery an approximation for this distance query
	 * (i.e. a query that produces no false-negatives, but may produce false-positives), or {@code null}.
	 * If non-null, only documents returned by the approximation query will be considered,
	 * which will enhance performance.
	 * @param centerCoordinates center of the search perimeter
	 * @param radius radius of the search perimeter
	 * @param coordinatesField name of the field implementing Coordinates
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public DistanceQuery(Query approximationQuery, Coordinates centerCoordinates, double radius, String coordinatesField) {
		this( approximationQuery, centerCoordinates, radius, coordinatesField, null, null );
	}

	/**
	 * Construct a distance query to match document distant at most of radius from center Point
	 *
	 * @param approximationQuery an approximation for this distance query
	 * (i.e. a query that produces no false-negatives, but may produce false-positives), or {@code null}.
	 * If non-null, only documents returned by the approximation query will be considered,
	 * which will enhance performance.
	 * @param centerCoordinates center of the search perimeter
	 * @param radius radius of the search perimeter
	 * @param latitudeField name of the field hosting latitude
	 * @param longitudeField name of the field hosting longitude
	 * @see org.hibernate.search.spatial.Coordinates
	 */
	public DistanceQuery(Query approximationQuery, Coordinates centerCoordinates, double radius, String latitudeField, String longitudeField) {
		this( approximationQuery, centerCoordinates, radius, null, latitudeField, longitudeField );
	}

	private DistanceQuery(Query approximationQuery, Coordinates centerCoordinates, double radius, String coordinatesField, String latitudeField, String longitudeField) {
		if ( approximationQuery == null ) {
			this.approximationQuery = new MatchAllDocsQuery();
		}
		else {
			this.approximationQuery = approximationQuery;
		}
		this.center = Point.fromCoordinates( centerCoordinates );
		this.radius = radius;
		this.coordinatesField = coordinatesField;
		this.latitudeField = latitudeField;
		this.longitudeField = longitudeField;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		Query superRewritten = super.rewrite( reader );
		if ( superRewritten != this ) {
			return superRewritten;
		}
		Query rewrittenApproximationQuery = approximationQuery.rewrite( reader );
		if ( rewrittenApproximationQuery != approximationQuery ) {
			DistanceQuery clone = new DistanceQuery( rewrittenApproximationQuery, this.center, this.radius, this.coordinatesField, this.latitudeField, this.longitudeField );
			return clone;
		}
		return this;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
		Weight approximationWeight = approximationQuery.createWeight( searcher, needsScores );
		return new ConstantScoreWeight( this ) {
			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				Scorer approximationScorer = approximationWeight.scorer( context );
				if ( approximationScorer == null ) {
					// No result
					return null;
				}
				DocIdSetIterator approximation = approximationScorer.iterator();
				TwoPhaseIterator iterator = createDocIdSetIterator( approximation, context );
				return new ConstantScoreScorer( this, score(), iterator );
			}
		};
	}

	/**
	 * Returns a {@link TwoPhaseIterator} that will first check the {@link #approximationQuery} (if any),
	 * and will only match documents whose coordinates are within distance(radius) of the center of the search.
	 *
	 * @param approximation an approximation of matching documents.
	 * @param context the {@link LeafReaderContext} for which to return the {LeafReaderContext}.
	 *
	 * @return a {@link TwoPhaseIterator} with the matching document ids
	 */
	private TwoPhaseIterator createDocIdSetIterator(DocIdSetIterator approximation, LeafReaderContext context) throws IOException {
		return new TwoPhaseIterator( approximation ) {

			private Bits docsWithLatitude;
			private Bits docsWithLongitude;
			private NumericDocValues latitudeValues;
			private NumericDocValues longitudeValues;

			private void lazyInit() throws IOException {
				if ( docsWithLatitude != null ) {
					return;
				}
				LeafReader atomicReader = context.reader();
				this.docsWithLatitude = DocValues.getDocsWithField( atomicReader, getLatitudeField() );
				this.docsWithLongitude = DocValues.getDocsWithField( atomicReader, getLongitudeField() );
				this.latitudeValues = DocValues.getNumeric( atomicReader, getLatitudeField() );
				this.longitudeValues = DocValues.getNumeric( atomicReader, getLongitudeField() );
			}

			@Override
			public boolean matches() throws IOException {
				lazyInit();
				int docID = approximation().docID();
				if ( docsWithLatitude.get( docID ) && docsWithLongitude.get( docID ) ) {
					double lat = coordinate( latitudeValues, docID );
					double lon = coordinate( longitudeValues, docID );
					if ( center.getDistanceTo( lat, lon ) <= radius ) {
						return true;
					}
				}
				return false;
			}

			@Override
			public float matchCost() {
				/*
				 * I honestly have no idea how many "simple operations" we're performing here.
				 * I suppose sines and cosines are very low-level, probably assembly instructions
				 * on most architectures.
				 * Some Lucene implementations seem to use 100 as a default, so let's do the same.
				 */
				return 100;
			}
		};
	}

	public String getCoordinatesField() {
		if ( coordinatesField != null ) {
			return coordinatesField;
		}
		else {
			return SpatialHelper.stripSpatialFieldSuffix( latitudeField );
		}
	}

	public double getRadius() {
		return radius;
	}

	public Point getCenter() {
		return center;
	}

	public Query getApproximationQuery() {
		return approximationQuery;
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
	public int hashCode() {
		int hashCode = 31 * super.hashCode() + approximationQuery.hashCode();
		hashCode = 31 * hashCode + center.hashCode();
		hashCode = 31 * hashCode + Double.hashCode( radius );
		hashCode = 31 * hashCode + Objects.hashCode( coordinatesField );
		hashCode = 31 * hashCode + Objects.hashCode( latitudeField );
		hashCode = 31 * hashCode + Objects.hashCode( longitudeField );
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj instanceof DistanceQuery ) {
			DistanceQuery other = (DistanceQuery) obj;
			return Float.floatToIntBits( getBoost() ) == Float.floatToIntBits( other.getBoost() )
				&& approximationQuery.equals( other.approximationQuery )
				&& center.equals( other.center )
				&& radius == other.radius
				&& Objects.equals( coordinatesField, other.coordinatesField )
				&& Objects.equals( latitudeField, other.latitudeField )
				&& Objects.equals( longitudeField, other.longitudeField );
		}
		return false;
	}

	@Override
	public String toString(String field) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "DistanceQuery" );
		sb.append( "{approximationQuery=" ).append( approximationQuery );
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
