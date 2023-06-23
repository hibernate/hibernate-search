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
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.IndexSearcher;

public class SearchWork<R> implements ReadWork<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearcher<R, ?> searcher;

	private final int offset;
	private final Integer limit;
	private final int totalHitCountThreshold;

	SearchWork(LuceneSearcher<R, ?> searcher,
			Integer offset, Integer limit,
			int totalHitCountThreshold) {
		this.offset = offset == null ? 0 : offset;
		this.limit = limit;
		this.searcher = searcher;
		this.totalHitCountThreshold = totalHitCountThreshold;
	}

	@Override
	public R execute(ReadWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = context.createSearcher();

			return searcher.search(
					indexSearcher, context.getIndexReaderMetadataResolver(), offset, limit, totalHitCountThreshold
			);
		}
		catch (IOException e) {
			throw log.ioExceptionOnQueryExecution( searcher.getLuceneQueryForExceptions(), e.getMessage(),
					context.getEventContext(), e );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "searcher=" ).append( searcher )
				.append( ", offset=" ).append( offset )
				.append( ", limit=" ).append( limit )
				.append( ", totalHitCountThreshold=" ).append( totalHitCountThreshold )
				.append( "]" );
		return sb.toString();
	}
}
