/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.HibernateSearchMultiCollectorManager;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;

public class AggregationExtractContext {

	private final LuceneSearchQueryIndexScope<?> queryIndexScope;
	private final BackendSessionContext sessionContext;
	private final IndexReader indexReader;
	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;
	private final HibernateSearchMultiCollectorManager.MultiCollectedResults multiCollectedResults;
	private final Set<String> routingKeys;

	public AggregationExtractContext(LuceneSearchQueryIndexScope<?> queryIndexScope, BackendSessionContext sessionContext,
			IndexReader indexReader,
			FromDocumentValueConvertContext fromDocumentValueConvertContext,
			HibernateSearchMultiCollectorManager.MultiCollectedResults multiCollectedResults, Set<String> routingKeys) {
		this.queryIndexScope = queryIndexScope;
		this.sessionContext = sessionContext;
		this.indexReader = indexReader;
		this.fromDocumentValueConvertContext = fromDocumentValueConvertContext;
		this.multiCollectedResults = multiCollectedResults;
		this.routingKeys = routingKeys;
	}

	public PredicateRequestContext toPredicateRequestContext(String absolutePath) {
		return PredicateRequestContext.withSession( queryIndexScope, sessionContext, routingKeys )
				.withNestedPath( absolutePath );
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
