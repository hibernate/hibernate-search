/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.NumericUtils;

/**
 * A {@link DoubleMultiValuesToSingleValuesSource} that wraps the distance between a GeoPoint field
 * and a given center.
 */
public class GeoPointDistanceMultiValuesToSingleValuesSource extends DoubleMultiValuesToSingleValuesSource {

	private final String field;
	private final GeoPoint center;

	public GeoPointDistanceMultiValuesToSingleValuesSource(String field, MultiValueMode mode,
			NestedDocsProvider nestedDocsProvider, GeoPoint center) {
		super( mode, nestedDocsProvider, NumericUtils::doubleToSortableLong );
		this.field = field;
		this.center = center;
	}

	@Override
	public String toString() {
		return "distance(" + field + "," + center + "," + mode + "," + nestedDocsProvider + ")";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !super.equals( o ) ) {
			return false;
		}
		GeoPointDistanceMultiValuesToSingleValuesSource that = (GeoPointDistanceMultiValuesToSingleValuesSource) o;
		return Objects.equals( field, that.field )
				&& Objects.equals( center, that.center );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), field, center );
	}

	@Override
	public boolean needsScores() {
		return false;
	}

	@Override
	public boolean isCacheable(LeafReaderContext ctx) {
		return DocValues.isCacheable( ctx, field );
	}

	@Override
	public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
		DoubleValues values = getValues( ctx, null );
		if ( values.advanceExact( docId ) ) {
			return Explanation.match( values.doubleValue(), this.toString() );
		}
		else {
			return Explanation.noMatch( this.toString() );
		}
	}

	@Override
	public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
		return this;
	}

	@Override
	protected GeoPointDistanceDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException {
		// Numeric doc values are longs, but we want doubles
		return new GeoPointDistanceDocValues( DocValues.getSortedNumeric( ctx.reader(), field ), center );
	}
}
