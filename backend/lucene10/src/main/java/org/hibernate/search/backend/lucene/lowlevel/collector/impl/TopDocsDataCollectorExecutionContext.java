/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class TopDocsDataCollectorExecutionContext extends CollectorExecutionContext {
	private final Query executedQuery;
	private final Query originalQuery;
	private final TopDocs topDocs;
	private final StoredFieldsValuesDelegate storedFieldsValuesDelegate;

	private IntIntMap docIdToScoreDocIndex;

	public TopDocsDataCollectorExecutionContext(IndexReaderMetadataResolver metadataResolver,
			IndexSearcher indexSearcher, Query executedQuery, Query originalQuery, TopDocs topDocs,
			StoredFieldsValuesDelegate.Factory storedFieldsValuesDelegateOrNull)
			throws IOException {
		super( metadataResolver, indexSearcher, topDocs.scoreDocs.length );
		this.executedQuery = executedQuery;
		this.originalQuery = originalQuery;
		this.topDocs = topDocs;
		this.storedFieldsValuesDelegate = storedFieldsValuesDelegateOrNull == null
				? null
				: storedFieldsValuesDelegateOrNull.create( this );
	}

	public Query executedQuery() {
		return executedQuery;
	}

	public Query originalQuery() {
		return originalQuery;
	}

	public TopDocs topDocs() {
		return topDocs;
	}

	public IntIntMap docIdToScoreDocIndex() {
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		if ( docIdToScoreDocIndex == null ) {
			docIdToScoreDocIndex = new IntIntHashMap();
			for ( int i = 0; i < scoreDocs.length; i++ ) {
				docIdToScoreDocIndex.put( scoreDocs[i].doc, i );
			}
		}
		return docIdToScoreDocIndex;
	}

	public StoredFieldsValuesDelegate storedFieldsValuesDelegate() {
		return storedFieldsValuesDelegate;
	}
}
