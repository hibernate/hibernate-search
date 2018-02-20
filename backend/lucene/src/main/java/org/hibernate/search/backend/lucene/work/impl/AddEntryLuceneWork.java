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
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.spi.Futures;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class AddEntryLuceneWork extends AbstractLuceneWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String id;

	private final LuceneIndexEntry indexEntry;

	public AddEntryLuceneWork(String indexName, String id, LuceneIndexEntry indexEntry) {
		super( "addEntry", indexName );
		this.id = id;
		this.indexEntry = indexEntry;
	}

	@Override
	public CompletableFuture<Long> execute(LuceneIndexWorkExecutionContext context) {
		return Futures.create( () -> addEntry( context.getIndexWriter() ) );
	}

	private CompletableFuture<Long> addEntry(IndexWriter indexWriter) {
		try {
			return CompletableFuture.completedFuture( indexWriter.addDocuments( indexEntry ) );
		}
		catch (IOException e) {
			throw log.unableToIndexEntry( indexName, id, e );
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
