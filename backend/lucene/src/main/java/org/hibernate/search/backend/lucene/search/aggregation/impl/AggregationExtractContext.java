/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.HibernateSearchMultiCollectorManager;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;

public class AggregationExtractContext {

	private final IndexReader indexReader;
	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;
	private final HibernateSearchMultiCollectorManager.MultiCollectedResults multiCollectedResults;

	public AggregationExtractContext(IndexReader indexReader,
			FromDocumentValueConvertContext fromDocumentValueConvertContext,
			HibernateSearchMultiCollectorManager.MultiCollectedResults multiCollectedResults) {
		this.indexReader = indexReader;
		this.fromDocumentValueConvertContext = fromDocumentValueConvertContext;
		this.multiCollectedResults = multiCollectedResults;
	}

	public IndexReader getIndexReader() {
		return indexReader;
	}

	public FromDocumentValueConvertContext fromDocumentValueConvertContext() {
		return fromDocumentValueConvertContext;
	}

	public <C extends Collector, T> T getFacets(CollectorKey<C, T> key) {
		return multiCollectedResults.get( key );
	}

	public NestedDocsProvider createNestedDocsProvider(String nestedDocumentPath, Query nestedFilter) {
		return new NestedDocsProvider( nestedDocumentPath, nestedFilter );
	}
}
