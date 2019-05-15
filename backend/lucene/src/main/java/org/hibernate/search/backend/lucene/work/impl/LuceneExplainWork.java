/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

class LuceneExplainWork implements LuceneReadWork<Explanation> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;

	private final Query luceneQuery;

	private final String indexName;
	private final String documentId;
	private final Query explainedDocumentQuery;

	LuceneExplainWork(Set<String> indexNames, Query luceneQuery,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentQuery) {
		this.indexNames = indexNames;
		this.luceneQuery = luceneQuery;
		this.indexName = explainedDocumentIndexName;
		this.documentId = explainedDocumentId;
		this.explainedDocumentQuery = explainedDocumentQuery;
	}

	@Override
	public CompletableFuture<Explanation> execute(LuceneReadWorkExecutionContext context) {
		// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexReader
		return Futures.create( () -> CompletableFuture.completedFuture( explain( context ) ) );
	}

	private Explanation explain(LuceneReadWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = new IndexSearcher( context.getIndexReader() );

			int luceneDocId = getLuceneDocId( indexSearcher );

			return indexSearcher.explain( luceneQuery, luceneDocId );
		}
		catch (IOException e) {
			throw log.ioExceptionOnQueryExecution( luceneQuery, context.getEventContext(), e );
		}
	}

	/*
	 * Find the Lucene docId for a specific document in the current searcher.
	 * From what I understand, the Lucene docId might change from one search to another,
	 * it's more or less an address in an array (?).
	 */
	private int getLuceneDocId(IndexSearcher indexSearcher) throws IOException {
		TopDocs topDocs = indexSearcher.search( explainedDocumentQuery, 1 );
		if ( topDocs.scoreDocs.length != 1 ) {
			throw log.explainUnkownDocument( indexName, documentId );
		}
		return topDocs.scoreDocs[0].doc;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( indexNames )
				.append( ", luceneQuery=" ).append( luceneQuery )
				.append( ", explainedDocumentQuery=" ).append( explainedDocumentQuery )
				.append( "]" );
		return sb.toString();
	}
}
