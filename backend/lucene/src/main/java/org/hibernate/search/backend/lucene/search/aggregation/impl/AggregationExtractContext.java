/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.LuceneCollectorKey;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;

public class AggregationExtractContext {

	private final IndexReader indexReader;
	private final FromDocumentFieldValueConvertContext convertContext;
	private final Map<LuceneCollectorKey<?>, Collector> collectors;

	public AggregationExtractContext(IndexReader indexReader,
			FromDocumentFieldValueConvertContext convertContext,
			Map<LuceneCollectorKey<?>, Collector> collectors) {
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

	@SuppressWarnings("unchecked")
	public <C extends Collector> C getCollector(LuceneCollectorKey<C> key) {
		return (C) collectors.get( key );
	}

}
