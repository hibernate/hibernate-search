/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

/**
 * The collector used when collecting data related to top docs.
 *
 * @param <T> The type of value collected for each top doc.
 */
public class TopDocsDataCollector<T> extends SimpleCollector {

	public interface Factory<T> extends CollectorKey<TopDocsDataCollector<T>> {

		TopDocsDataCollector<T> create(TopDocsDataCollectorExecutionContext context) throws IOException;

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

	public T get(int doc) {
		return collected.get( doc );
	}
}
