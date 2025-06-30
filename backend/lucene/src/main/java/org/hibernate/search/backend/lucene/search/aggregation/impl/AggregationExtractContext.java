/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;

public interface AggregationExtractContext {
	PredicateRequestContext toPredicateRequestContext(String absolutePath);

	IndexReader getIndexReader();

	FromDocumentValueConvertContext fromDocumentValueConvertContext();

	<C extends Collector, T> T getCollectorResults(CollectorKey<C, T> key);

	NestedDocsProvider createNestedDocsProvider(String nestedDocumentPath, Query nestedFilter);
}
