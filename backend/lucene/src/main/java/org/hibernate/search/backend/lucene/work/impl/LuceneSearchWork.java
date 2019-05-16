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
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectors;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneLoadableSearchResult;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchResultExtractor;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchWork<H> implements LuceneReadWork<LuceneLoadableSearchResult<H>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;

	private final Query luceneQuery;
	private final Sort luceneSort;

	private final int offset;
	private final Integer limit;

	private final LuceneCollectorProvider luceneCollectorProvider;
	private final LuceneSearchResultExtractor<H> searchResultExtractor;

	LuceneSearchWork(Set<String> indexNames,
			Query luceneQuery,
			Sort luceneSort,
			Integer offset,
			Integer limit,
			LuceneCollectorProvider luceneCollectorProvider,
			LuceneSearchResultExtractor<H> searchResultExtractor) {
		this.indexNames = indexNames;
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.offset = offset == null ? 0 : offset;
		this.limit = limit;
		this.luceneCollectorProvider = luceneCollectorProvider;
		this.searchResultExtractor = searchResultExtractor;
	}

	@Override
	public CompletableFuture<LuceneLoadableSearchResult<H>> execute(LuceneQueryWorkExecutionContext context) {
		// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexReader
		return Futures.create( () -> CompletableFuture.completedFuture( executeQuery( context ) ) );
	}

	private LuceneLoadableSearchResult<H> executeQuery(LuceneQueryWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = new IndexSearcher( context.getIndexReader() );

			// TODO GSM implement timeout handling by wrapping the collector with the timeout limiting one
			LuceneCollectorsBuilder luceneCollectorsBuilder = new LuceneCollectorsBuilder( luceneSort, getMaxDocs( context ) );
			luceneCollectorProvider.contributeCollectors( luceneCollectorsBuilder );
			LuceneCollectors luceneCollectors = luceneCollectorsBuilder.build();

			luceneCollectors.collect( indexSearcher, luceneQuery, offset, limit );

			SearchProjectionExtractContext projectionExecutionContext =
					new SearchProjectionExtractContext( indexSearcher, luceneQuery, luceneCollectors.getDistanceCollectors() );

			return searchResultExtractor.extract(
					indexSearcher, luceneCollectors.getTotalHits(),
					luceneCollectors.getTopDocs(),
					projectionExecutionContext
			);
		}
		catch (IOException e) {
			throw log.ioExceptionOnQueryExecution( luceneQuery, context.getEventContext(), e );
		}
	}

	private int getMaxDocs(LuceneQueryWorkExecutionContext context) {
		IndexReader reader = context.getIndexReader();
		// FIXME this is very naive for now, we will probably need to implement some scrolling in the collector
		// as it is done in Search 5.
		// Note that Lucene initializes data structures of this size so setting it to a large value consumes memory.
		if ( limit == null ) {
			return reader.maxDoc();
		}
		else if ( limit == 0L ) {
			return 0;
		}
		else {
			return Math.min( (int) ( offset + limit ), reader.maxDoc() );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexNames=" ).append( indexNames )
				.append( ", luceneQuery=" ).append( luceneQuery )
				.append( ", luceneSort=" ).append( luceneSort )
				.append( ", offset=" ).append( offset )
				.append( ", limit=" ).append( limit )
				.append( "]" );
		return sb.toString();
	}
}
