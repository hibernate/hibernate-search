/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFields;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.util.spi.Futures;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class UpdateEntryLuceneWork extends AbstractLuceneWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;

	private final String id;

	private final LuceneIndexEntry indexEntry;

	public UpdateEntryLuceneWork(String indexName, String tenantId, String id, LuceneIndexEntry indexEntry) {
		super( "updateEntry", indexName );
		this.tenantId = tenantId;
		this.id = id;
		this.indexEntry = indexEntry;
	}

	@Override
	public CompletableFuture<Long> execute(LuceneIndexWorkExecutionContext context) {
		return Futures.create( () -> CompletableFuture.completedFuture( updateEntry( context.getIndexWriter() ) ) );
	}

	private long updateEntry(IndexWriter indexWriter) {
		try {
			if ( tenantId == null ) {
				// if the tenantId is null, we can do an atomic update
				// we don't expose the query construction in LuceneQueries as it is not considered safe and should be
				// used with care
				return indexWriter.updateDocuments( new Term( LuceneFields.idFieldName(), id ), indexEntry );
			}
			else {
				indexWriter.deleteDocuments( LuceneQueries.documentIdQuery( tenantId, id ) );
				return indexWriter.addDocuments( indexEntry );
			}
		}
		catch (IOException e) {
			throw log.unableToIndexEntry( indexName, tenantId, id, e );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", entry=" ).append( indexEntry )
				.append( "]" );
		return sb.toString();
	}
}
