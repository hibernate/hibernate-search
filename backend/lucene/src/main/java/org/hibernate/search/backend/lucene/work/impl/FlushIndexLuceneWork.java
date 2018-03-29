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
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class FlushIndexLuceneWork extends AbstractLuceneWork<Void> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public FlushIndexLuceneWork(String indexName) {
		super( "flushIndex", indexName );
	}

	@Override
	public CompletableFuture<Void> execute(LuceneIndexWorkExecutionContext context) {
		return Futures.create( () -> CompletableFuture.completedFuture( null ).thenRun( () -> flushIndex( context.getIndexWriter() ) ) );
	}

	private void flushIndex(IndexWriter indexWriter) {
		try {
			indexWriter.flush();
		}
		catch (IOException e) {
			throw log.unableToFlushIndex( indexName, e );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", indexName=" ).append( indexName )
				.append( "]" );
		return sb.toString();
	}
}
