/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.facet.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.range.Range;
import org.apache.lucene.search.Query;

/**
 * <p>
 * Copied with some changes from {@code org.apache.lucene.facet.range.RangeFacetCounts}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public class MultiValueRangeFacetCounts extends Facets {

	protected final Range[] ranges;
	protected final int[] counts;
	protected final Query fastMatchQuery;
	protected final String field;
	protected int totCount;

	protected MultiValueRangeFacetCounts(String field, Range[] ranges, Query fastMatchQuery) {
		this.field = field;
		this.ranges = ranges;
		this.fastMatchQuery = fastMatchQuery;
		counts = new int[ranges.length];
	}

	@Override
	public FacetResult getAllChildren(String dim, String... path) {
		throw new UnsupportedOperationException(
				"Getting all children is not supported by " + this.getClass().getSimpleName() );
	}

	@Override
	public FacetResult getTopChildren(int topN, String dim, String... path) {
		if ( !dim.equals( field ) ) {
			throw new IllegalArgumentException( "invalid dim \"" + dim + "\"; should be \"" + field + "\"" );
		}
		if ( path.length != 0 ) {
			throw new IllegalArgumentException( "path.length should be 0" );
		}
		LabelAndValue[] labelValues = new LabelAndValue[counts.length];
		for ( int i = 0; i < counts.length; i++ ) {
			labelValues[i] = new LabelAndValue( ranges[i].label, counts[i] );
		}
		return new FacetResult( dim, path, totCount, labelValues, labelValues.length );
	}

	@Override
	public Number getSpecificValue(String dim, String... path) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FacetResult> getAllDims(int topN) throws IOException {
		return Collections.singletonList( getTopChildren( topN, field ) );
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append( "MultiValueRangeFacetCounts totCount=" );
		b.append( totCount );
		b.append( ":\n" );
		for ( int i = 0; i < ranges.length; i++ ) {
			b.append( "  " );
			b.append( ranges[i].label );
			b.append( " -> count=" );
			b.append( counts[i] );
			b.append( '\n' );
		}
		return b.toString();
	}
}
