/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.MappedTypeNameQuery;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

class ExplainWork implements ReadWork<Explanation> {

	private final LuceneSearcher<?, ?> searcher;

	private final String explainedDocumentTypeName;
	private final String explainedDocumentId;
	private final Query explainedDocumentFilter;

	ExplainWork(LuceneSearcher<?, ?> searcher,
			String explainedDocumentTypeName, String explainedDocumentId, Query explainedDocumentFilter) {
		this.searcher = searcher;
		this.explainedDocumentTypeName = explainedDocumentTypeName;
		this.explainedDocumentId = explainedDocumentId;
		this.explainedDocumentFilter = explainedDocumentFilter;
	}

	@Override
	public Explanation execute(ReadWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = context.createSearcher();

			int luceneDocId = getLuceneDocId( context, indexSearcher );

			return searcher.explain( indexSearcher, luceneDocId );
		}
		catch (IOException e) {
			throw QueryLog.INSTANCE.ioExceptionOnQueryExecution( searcher.getLuceneQueryForExceptions(), e.getMessage(),
					context.getEventContext(), e );
		}
	}

	/*
	 * Find the Lucene docId for a specific document in the current searcher.
	 * From what I understand, the Lucene docId might change from one search to another,
	 * it's more or less an address in an array (?).
	 */
	private int getLuceneDocId(ReadWorkExecutionContext context, IndexSearcher indexSearcher) throws IOException {
		Query explainedDocumentQuery = createExplainedDocumentQuery( context );

		TopDocs topDocs = indexSearcher.search( explainedDocumentQuery, 2 );
		if ( topDocs.scoreDocs.length < 1 ) {
			throw QueryLog.INSTANCE.explainUnknownDocument( explainedDocumentTypeName, explainedDocumentId );
		}
		if ( topDocs.scoreDocs.length > 1 ) {
			throw new AssertionFailure(
					"Multiple documents match query " + explainedDocumentQuery + "."
			);
		}
		return topDocs.scoreDocs[0].doc;
	}

	private Query createExplainedDocumentQuery(ReadWorkExecutionContext context) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder()
				.add( Queries.mainDocumentQuery(), BooleanClause.Occur.FILTER )
				.add( Queries.term( MetadataFields.idFieldName(), explainedDocumentId ), BooleanClause.Occur.FILTER )
				.add( new MappedTypeNameQuery( context.getIndexReaderMetadataResolver(), explainedDocumentTypeName ),
						BooleanClause.Occur.FILTER );
		if ( explainedDocumentFilter != null ) {
			builder.add( explainedDocumentFilter, BooleanClause.Occur.FILTER );
		}
		return builder.build();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "searcher=" ).append( searcher )
				.append( ", explainedDocumentIndexName=" ).append( explainedDocumentTypeName )
				.append( ", explainedDocumentId=" ).append( explainedDocumentId )
				.append( ", explainedDocumentFilter=" ).append( explainedDocumentFilter )
				.append( "]" );
		return sb.toString();
	}
}
