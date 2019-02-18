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

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexWriter;

public abstract class AbstractLuceneDeleteAllEntriesWork extends AbstractLuceneWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;

	public AbstractLuceneDeleteAllEntriesWork(String indexName, String tenantId) {
		super( "deleteAllEntries", indexName );
		this.tenantId = tenantId;
	}

	@Override
	public CompletableFuture<Long> execute(LuceneIndexWorkExecutionContext context) {
		return Futures.create( () -> CompletableFuture.completedFuture( deleteDocuments( context.getIndexWriter() ) ) );
	}

	private Long deleteDocuments(IndexWriter indexWriter) {
		try {
			return doDeleteDocuments( indexWriter, tenantId );
		}
		catch (IOException e) {
			throw log.unableToDeleteAllEntriesFromIndex( tenantId, getEventContext(), e );
		}
	}

	protected abstract long doDeleteDocuments(IndexWriter indexWriter, String tenantId) throws IOException;

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
