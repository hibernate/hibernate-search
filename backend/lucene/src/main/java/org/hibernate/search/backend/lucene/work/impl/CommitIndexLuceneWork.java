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
public class CommitIndexLuceneWork extends AbstractLuceneWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public CommitIndexLuceneWork(String indexName) {
		super( "commitIndex", indexName );
	}

	@Override
	public CompletableFuture<Long> execute(LuceneIndexWorkExecutionContext context) {
		// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexWriter
		return Futures.create( () -> commitIndex( context.getIndexWriter() ) );
	}

	private CompletableFuture<Long> commitIndex(IndexWriter indexWriter) {
		try {
			return CompletableFuture.completedFuture( indexWriter.commit() );
		}
		catch (IOException e) {
			throw log.unableToCommitIndex( indexName, e );
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