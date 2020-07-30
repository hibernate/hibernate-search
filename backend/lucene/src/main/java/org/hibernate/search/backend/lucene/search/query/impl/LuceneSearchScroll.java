/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSyncWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearcher;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.backend.lucene.work.impl.ReadWork;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchScrollResult;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneSearchScroll<H> implements SearchScroll<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// shared with its query instance:
	private final LuceneSyncWorkOrchestrator queryOrchestrator;
	private final LuceneWorkFactory workFactory;
	private final LuceneSearchContext searchContext;
	private final Set<String> routingKeys;
	private final TimeoutManager timeoutManager;
	private final LuceneSearcher<LuceneLoadableSearchResult<H>, LuceneExtractableSearchResult<H>> searcher;

	// specific to this scroll instance:
	private final HibernateSearchMultiReader indexReader;
	private final int pageSize;

	private int scrollIndex = 0;

	public LuceneSearchScroll(LuceneSyncWorkOrchestrator queryOrchestrator,
			LuceneWorkFactory workFactory, LuceneSearchContext searchContext,
			Set<String> routingKeys,
			TimeoutManager timeoutManager,
			LuceneSearcher<LuceneLoadableSearchResult<H>, LuceneExtractableSearchResult<H>> searcher,
			HibernateSearchMultiReader indexReader, int pageSize
			) {
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.searchContext = searchContext;
		this.routingKeys = routingKeys;
		this.timeoutManager = timeoutManager;
		this.searcher = searcher;
		this.indexReader = indexReader;
		this.pageSize = pageSize;
	}

	@Override
	public void close() {
		try {
			indexReader.close();
		}
		catch (IOException | RuntimeException e) {
			log.unableToCloseIndexReader( EventContexts.fromIndexNames( searchContext.indexes().indexNames() ), e );
		}
	}

	@Override
	public SearchScrollResult<H> next() {
		timeoutManager.start();

		ReadWork<LuceneLoadableSearchResult<H>> work = workFactory.search( searcher, scrollIndex, pageSize );
		LuceneLoadableSearchResult<H> search = doSubmitWithIndexReader( work, indexReader );
		LuceneSearchResult<H> result = search
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async processing, we will have to add a nextAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking();

		timeoutManager.stop();

		// increasing the index for further next(s)
		scrollIndex += pageSize;
		return new SimpleSearchScrollResult<>( search.hasHits(), result.hits() );
	}

	private <T> T doSubmitWithIndexReader(ReadWork<T> work, HibernateSearchMultiReader indexReader) {
		return queryOrchestrator.submit(
				searchContext.indexes().indexNames(),
				searchContext.indexes().elements(),
				routingKeys,
				work, indexReader
		);
	}
}
