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
	private final CollectorSet collectors;

	public AggregationExtractContext(LuceneSearchQueryIndexScope<?> queryIndexScope, BackendSessionContext sessionContext,
			IndexReader indexReader,
			FromDocumentValueConvertContext fromDocumentValueConvertContext,
			CollectorSet collectors) {
		this.queryIndexScope = queryIndexScope;
		this.sessionContext = sessionContext;
		this.indexReader = indexReader;
		this.fromDocumentValueConvertContext = fromDocumentValueConvertContext;
		this.collectors = collectors;
	}

	public PredicateRequestContext toPredicateRequestContext(String absolutePath) {
		return PredicateRequestContext.withSession( queryIndexScope, sessionContext ).withNestedPath( absolutePath );
	}

	public IndexReader getIndexReader() {
		return indexReader;
	}

	public FromDocumentValueConvertContext fromDocumentValueConvertContext() {
		return fromDocumentValueConvertContext;
	}

	public <C extends Collector> C getCollector(CollectorKey<C> key) {
		return collectors.get( key );
	}

	public NestedDocsProvider createNestedDocsProvider(String nestedDocumentPath, Query nestedFilter) {
		return new NestedDocsProvider( nestedDocumentPath, nestedFilter );
	}
}
