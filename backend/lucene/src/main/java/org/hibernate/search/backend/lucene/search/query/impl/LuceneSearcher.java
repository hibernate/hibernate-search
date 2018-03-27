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
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.engine.search.SearchResult;

/**
 * @author Guillaume Smet
 */
public class LuceneSearcher<T> implements AutoCloseable {

	private final Set<String> indexNames;
	private final IndexSearcher indexSearcher;

	private final Query luceneQuery;
	private final Sort luceneSort;

	private final Long firstResultIndex;
	private final Long maxResultsCount;

	private final HitExtractor<?> hitExtractor;
	private final SearchResultExtractor<T> searchResultExtractor;

	public LuceneSearcher(Set<String> indexNames,
			Set<ReaderProvider> readerProviders,
			Query luceneQuery,
			Sort luceneSort,
			Long firstResultIndex,
			Long maxResultsCount,
			HitExtractor<?> hitExtractor,
			SearchResultExtractor<T> searchResultExtractor) {
		this.indexNames = indexNames;
		this.indexSearcher = new IndexSearcher( MultiReaderFactory.openReader( indexNames, readerProviders ) );
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.firstResultIndex = firstResultIndex;
		this.maxResultsCount = maxResultsCount;
		this.hitExtractor = hitExtractor;
		this.searchResultExtractor = searchResultExtractor;
	}

	public SearchResult<T> execute() throws IOException {
		// TODO GSM implement timeout handling by wrapping the collector with the timeout limiting one

		LuceneCollectorsBuilder luceneCollectorsBuilder = new LuceneCollectorsBuilder( luceneSort, getMaxDocs() );
		hitExtractor.contributeCollectors( luceneCollectorsBuilder );
		LuceneCollectors luceneCollectors = luceneCollectorsBuilder.build();

		indexSearcher.search( luceneQuery, luceneCollectors.getCompositeCollector() );

		return searchResultExtractor.extract( indexSearcher, getTopDocs( luceneCollectors ) );
	}

	public Query getLuceneQuery() {
		return luceneQuery;
	}

	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public void close() {
		MultiReaderFactory.closeReader( indexSearcher.getIndexReader() );
	}

	private int getMaxDocs() {
		// FIXME this is very naive for now, we will probably need to implement some scrolling in the collector
		// as it is done in Search 5.
		// Note that Lucene initializes data structures of this size so setting it to a large value consumes memory.
		if ( maxResultsCount == null ) {
			return indexSearcher.getIndexReader().maxDoc();
		}
		else if ( maxResultsCount == 0L ) {
			return 0;
		}
		else {
			return Math.min( (int) ( firstResultIndex + maxResultsCount ), indexSearcher.getIndexReader().maxDoc() );
		}
	}

	private TopDocs getTopDocs(LuceneCollectors luceneCollectors) {
		if ( maxResultsCount == null ) {
			return luceneCollectors.getTopDocsCollector().topDocs( firstResultIndex.intValue() );
		}
		else {
			return luceneCollectors.getTopDocsCollector().topDocs( firstResultIndex.intValue(), maxResultsCount.intValue() );
		}
	}
}
