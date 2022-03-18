/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.CollectorSet;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;

public class AggregationExtractContext {

	private final IndexReader indexReader;
	private final FromDocumentFieldValueConvertContext convertContext;
	private final CollectorSet collectors;

	public AggregationExtractContext(IndexReader indexReader,
			FromDocumentFieldValueConvertContext convertContext,
			CollectorSet collectors) {
		this.indexReader = indexReader;
		this.convertContext = convertContext;
		this.collectors = collectors;
	}

	public IndexReader getIndexReader() {
		return indexReader;
	}

	public FromDocumentFieldValueConvertContext getConvertContext() {
		return convertContext;
	}

	public <C extends Collector> C getCollector(CollectorKey<C> key) {
		return collectors.get( key );
	}

	public NestedDocsProvider createNestedDocsProvider(String nestedDocumentPath, Query nestedFilter) {
		return new NestedDocsProvider( nestedDocumentPath, nestedFilter );
	}
}
