/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;

class LocalAggregationExtractContext implements AggregationExtractContext {

	private final AggregationExtractContext delegate;

	private Map<CollectorKey<?, ?>, Object> results;

	LocalAggregationExtractContext(AggregationExtractContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public PredicateRequestContext toPredicateRequestContext(String absolutePath) {
		return delegate.toPredicateRequestContext( absolutePath );
	}

	@Override
	public IndexReader getIndexReader() {
		return delegate.getIndexReader();
	}

	@Override
	public FromDocumentValueConvertContext fromDocumentValueConvertContext() {
		return delegate.fromDocumentValueConvertContext();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C extends Collector, T> T getCollectorResults(CollectorKey<C, T> key) {
		return (T) results.get( key );
	}

	@Override
	public NestedDocsProvider createNestedDocsProvider(String nestedDocumentPath, Query nestedFilter) {
		return delegate.createNestedDocsProvider( nestedDocumentPath, nestedFilter );
	}

	public void setResults(Map<CollectorKey<?, ?>, Object> results) {
		this.results = results;
	}
}
