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
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearcher;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class ExecuteQueryLuceneWork<T> implements LuceneQueryWork<SearchResult<T>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearcher<T> searcher;

	public ExecuteQueryLuceneWork(LuceneSearcher<T> searcher) {
		this.searcher = searcher;
	}

	@Override
	public CompletableFuture<SearchResult<T>> execute(LuceneQueryWorkExecutionContext context) {
		// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexWriter
		return Futures.create( () -> CompletableFuture.completedFuture( executeQuery( searcher ) ) );
	}

	private SearchResult<T> executeQuery(LuceneSearcher<T> searcher) {
		try {
			return searcher.execute();
		}
		catch (IOException e) {
			throw log.ioExceptionOnQueryExecution( searcher.getLuceneQuery(), searcher.getIndexNames(), e );
		}
		finally {
			searcher.close();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "searcher=" ).append( searcher )
				.append( "]" );
		return sb.toString();
	}
}
