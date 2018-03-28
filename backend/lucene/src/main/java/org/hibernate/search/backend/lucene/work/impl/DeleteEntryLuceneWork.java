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
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFields;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.util.spi.Futures;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class DeleteEntryLuceneWork extends AbstractLuceneWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;

	private final String id;

	public DeleteEntryLuceneWork(String indexName, String tenantId, String id) {
		super( "deleteEntry", indexName );
		this.tenantId = tenantId;
		this.id = id;
	}

	@Override
	public CompletableFuture<Long> execute(LuceneIndexWorkExecutionContext context) {
		// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexWriter
		return Futures.create( () -> CompletableFuture.completedFuture( deleteDocuments( context.getIndexWriter() ) ) );
	}

	private Long deleteDocuments(IndexWriter indexWriter) {
		try {
			if ( tenantId == null ) {
				return indexWriter.deleteDocuments( new Term( LuceneFields.idFieldName(), id ) );
			}
			else {
				return indexWriter.deleteDocuments( LuceneQueries.documentIdQuery( tenantId, id ) );
			}
		}
		catch (IOException e) {
			throw log.unableToDeleteEntryFromIndex( indexName, tenantId, id, e );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", indexName=" ).append( indexName )
				.append( ", id=" ).append( id )
				.append( "]" );
		return sb.toString();
	}
}
