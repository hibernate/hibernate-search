/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class CountTextValuesCollector extends SimpleCollector {

	private final TextMultiValuesSource source;
	private TextMultiValues values;
	private long count;
	LeafReaderContext context;

	public CountTextValuesCollector(TextMultiValuesSource source) {
		this.source = source;
	}

	@Override
	public void collect(int doc) throws IOException {
		if ( values.advanceExact( doc ) ) {
			while ( values.hasNextValue() ) {
				values.nextOrd();
				count++;
			}
		}
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		this.values = source.getValues( context );
	}


	@Override
	public void finish() throws IOException {
		this.values = null;
	}

	public long count() {
		return count;
	}
}
