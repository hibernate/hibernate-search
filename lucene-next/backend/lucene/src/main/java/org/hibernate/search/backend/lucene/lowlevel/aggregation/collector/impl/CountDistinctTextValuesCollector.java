/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.TextMultiValuesSource;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.cursors.LongCursor;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

public class CountDistinctTextValuesCollector extends SimpleCollector {

	private final TextMultiValuesSource source;
	private final String field;
	private TextMultiValues values;

	private final LongHashSet globalOrds = new LongHashSet();

	private LongHashSet leafOrds = new LongHashSet();

	private SortedSetDocValues sortedSetValues;

	public CountDistinctTextValuesCollector(TextMultiValuesSource source, String field) {
		this.source = source;
		this.field = field;
	}

	@Override
	public void collect(int doc) throws IOException {
		if ( values.advanceExact( doc ) ) {
			while ( values.hasNextValue() ) {
				long ord = values.nextOrd();
				leafOrds.add( ord );
			}
		}
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		initRootSortedSetDocValues( context );
		this.values = source.getValues( context );
	}

	@Override
	public void finish() throws IOException {
		for ( LongCursor value : leafOrds ) {
			long globalOrd = sortedSetValues.lookupTerm( values.lookupOrd( value.value ) );
			globalOrds.add( globalOrd );
		}
		values = null;
		leafOrds.clear();
	}

	private void initRootSortedSetDocValues(IndexReaderContext ctx) throws IOException {
		if ( sortedSetValues != null || ctx == null ) {
			return;
		}
		if ( ctx.isTopLevel ) {
			this.sortedSetValues = MultiDocValues.getSortedSetValues( ctx.reader(), field );
		}
		initRootSortedSetDocValues( ctx.parent );
	}

	public LongHashSet globalOrds() {
		return globalOrds;
	}
}
