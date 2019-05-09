/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectors;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * @param <H> The type of query hits.
 */
public class LuceneSearcher<H> implements AutoCloseable {

	private final Set<String> indexNames;
	private final IndexSearcher indexSearcher;

	private final Query luceneQuery;
	private final Sort luceneSort;

	private final long offset;
	private final Long limit;

	private final LuceneCollectorProvider luceneCollectorProvider;
	private final LuceneSearchResultExtractor<H> searchResultExtractor;

	public LuceneSearcher(Set<String> indexNames,
			Set<ReaderProvider> readerProviders,
			Query luceneQuery,
			Sort luceneSort,
			Long offset,
			Long limit,
			LuceneCollectorProvider luceneCollectorProvider,
			LuceneSearchResultExtractor<H> searchResultExtractor) {
		this.indexNames = indexNames;
		this.indexSearcher = new IndexSearcher( MultiReaderFactory.openReader( indexNames, readerProviders ) );
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.offset = offset == null ? 0L : offset;
		this.limit = limit;
		this.luceneCollectorProvider = luceneCollectorProvider;
		this.searchResultExtractor = searchResultExtractor;
	}

	public LuceneLoadableSearchResult<H> execute() throws IOException {
		// TODO GSM implement timeout handling by wrapping the collector with the timeout limiting one

		LuceneCollectorsBuilder luceneCollectorsBuilder = new LuceneCollectorsBuilder( luceneSort, getMaxDocs() );
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

	public Query getLuceneQuery() {
		return luceneQuery;
	}

	public EventContext getEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	@Override
	public void close() {
		MultiReaderFactory.closeReader( indexSearcher.getIndexReader() );
	}

	private int getMaxDocs() {
		// FIXME this is very naive for now, we will probably need to implement some scrolling in the collector
		// as it is done in Search 5.
		// Note that Lucene initializes data structures of this size so setting it to a large value consumes memory.
		if ( limit == null ) {
			return indexSearcher.getIndexReader().maxDoc();
		}
		else if ( limit == 0L ) {
			return 0;
		}
		else {
			return Math.min( (int) ( offset + limit ), indexSearcher.getIndexReader().maxDoc() );
		}
	}
}
