/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

/**
 * The collector used when collecting data related to top docs.
 *
 * @param <T> The type of value collected for each top doc.
 */
public class TopDocsDataCollector<T> extends SimpleCollector {

	public interface Factory<T> extends CollectorKey<TopDocsDataCollector<T>, IntObjectMap<T>> {

		CollectorManager<TopDocsDataCollector<T>, IntObjectMap<T>> create(TopDocsDataCollectorExecutionContext context)
				throws IOException;

	}

	private final Values<? extends T> values;
	private final StoredFieldsValuesDelegate storedFieldsValuesDelegate;

	private final IntObjectMap<T> collected = new IntObjectHashMap<>();
	private int currentLeafDocBase;

	public TopDocsDataCollector(TopDocsDataCollectorExecutionContext context, Values<? extends T> values) {
		this.values = values;
		this.storedFieldsValuesDelegate = context.storedFieldsValuesDelegate();
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		if ( storedFieldsValuesDelegate != null ) {
			storedFieldsValuesDelegate.context( context );
		}
		values.context( context );
		this.currentLeafDocBase = context.docBase;
	}

	@Override
	public void collect(int doc) throws IOException {
		if ( storedFieldsValuesDelegate != null ) {
			// Pre-load the stored fields of the current document for use in our Values.
			storedFieldsValuesDelegate.collect( doc );
		}
		collected.put( currentLeafDocBase + doc, values.get( doc ) );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	public IntObjectMap<T> collected() {
		return collected;
	}
}
