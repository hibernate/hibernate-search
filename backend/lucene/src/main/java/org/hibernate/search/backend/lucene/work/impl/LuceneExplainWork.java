/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.MappedTypeNameQuery;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

class LuceneExplainWork implements LuceneReadWork<Explanation> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearcher<?> searcher;

	private final String explainedDocumentIndexName;
	private final String explainedDocumentId;
	private final Query explainedDocumentFilter;

	LuceneExplainWork(LuceneSearcher<?> searcher,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentFilter) {
		this.searcher = searcher;
		this.explainedDocumentIndexName = explainedDocumentIndexName;
		this.explainedDocumentId = explainedDocumentId;
		this.explainedDocumentFilter = explainedDocumentFilter;
	}

	@Override
	public Explanation execute(LuceneReadWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = new IndexSearcher( context.getIndexReader() );

			int luceneDocId = getLuceneDocId( context, indexSearcher );

			return searcher.explain( indexSearcher, luceneDocId );
		}
		catch (IOException e) {
			throw log.ioExceptionOnQueryExecution( searcher.getLuceneQueryForExceptions(), context.getEventContext(), e );
		}
	}

	/*
	 * Find the Lucene docId for a specific document in the current searcher.
	 * From what I understand, the Lucene docId might change from one search to another,
	 * it's more or less an address in an array (?).
	 */
	private int getLuceneDocId(LuceneReadWorkExecutionContext context, IndexSearcher indexSearcher) throws IOException {
		Query explainedDocumentQuery = createExplainedDocumentQuery( context );

		TopDocs topDocs = indexSearcher.search( explainedDocumentQuery, 2 );
		if ( topDocs.scoreDocs.length < 1 ) {
			throw log.explainUnkownDocument( explainedDocumentIndexName, explainedDocumentId );
		}
		if ( topDocs.scoreDocs.length > 1 ) {
			throw new AssertionFailure(
					"Multiple documents match query " + explainedDocumentQuery + "." +
							" There is a bug in Hibernate Search, please report it."
			);
		}
		return topDocs.scoreDocs[0].doc;
	}

	private Query createExplainedDocumentQuery(LuceneReadWorkExecutionContext context) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder()
				.add( new TermQuery( new Term( LuceneFields.idFieldName(), explainedDocumentId ) ), BooleanClause.Occur.FILTER )
				.add( new MappedTypeNameQuery( context.getIndexReaderMetadataResolver(), explainedDocumentIndexName ), BooleanClause.Occur.FILTER );
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
				.append( ", explainedDocumentIndexName=" ).append( explainedDocumentIndexName )
				.append( ", explainedDocumentId=" ).append( explainedDocumentId )
				.append( ", explainedDocumentFilter=" ).append( explainedDocumentFilter )
				.append( "]" );
		return sb.toString();
	}
}
