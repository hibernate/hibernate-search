/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSyncWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchScroll;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchScrollResult;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearcher;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.backend.lucene.work.impl.ReadWork;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneSearchScrollImpl<H> implements LuceneSearchScroll<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// shared with its query instance:
	private final LuceneSyncWorkOrchestrator queryOrchestrator;
	private final LuceneWorkFactory workFactory;
	private final LuceneSearchQueryIndexScope<?> scope;
	private final Set<String> routingKeys;
	private final TimeoutManager timeoutManager;
	private final LuceneSearcher<LuceneLoadableSearchResult<H>, LuceneExtractableSearchResult<H>> searcher;
	private final int totalHitCountThreshold;

	// specific to this scroll instance:
	private final HibernateSearchMultiReader indexReader;
	private final int chunkSize;

	private int nextChunkOffset = 0;
	private int currentPageLimit;
	private LuceneExtractableSearchResult<H> currentPage;
	private int currentPageOffset = 0;

	public LuceneSearchScrollImpl(LuceneSyncWorkOrchestrator queryOrchestrator,
			LuceneWorkFactory workFactory, LuceneSearchQueryIndexScope<?> scope,
			Set<String> routingKeys,
			TimeoutManager timeoutManager,
			LuceneSearcher<LuceneLoadableSearchResult<H>, LuceneExtractableSearchResult<H>> searcher,
			int totalHitCountThreshold,
			HibernateSearchMultiReader indexReader, int chunkSize) {
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.scope = scope;
		this.routingKeys = routingKeys;
		this.timeoutManager = timeoutManager;
		this.searcher = searcher;
		this.totalHitCountThreshold = totalHitCountThreshold;
		this.indexReader = indexReader;
		this.chunkSize = chunkSize;
		this.currentPageLimit = chunkSize * 4; // Will fetch the topdocs for the first 4 pages initially
	}

	@Override
	public void close() {
		try {
			indexReader.close();
		}
		catch (IOException | RuntimeException e) {
			log.unableToCloseIndexReader( EventContexts.fromIndexNames( scope.hibernateSearchIndexNames() ), e );
		}
	}

	@Override
	public LuceneSearchScrollResult<H> next() {
		timeoutManager.start();
		try {
			return doNext();
		}
		finally {
			timeoutManager.stop();
		}
	}

	private LuceneSearchScrollResult<H> doNext() {
		if ( currentPage == null || nextChunkOffset + chunkSize > currentPageLimit + currentPageOffset ) {
			if ( currentPage != null ) {
				currentPageLimit *= 2;
			}
			currentPageOffset = nextChunkOffset;
			currentPage = doSubmitWithIndexReader(
					workFactory.scroll( searcher, currentPageOffset, currentPageLimit, totalHitCountThreshold ),
					indexReader );
		}

		int nextChunkStartIndexInPage = nextChunkOffset - currentPageOffset;

		// no more results check
		if ( nextChunkStartIndexInPage >= currentPage.hitSize() ) {
			return new LuceneSearchScrollResultImpl<>( currentPage.total(), false, Collections.emptyList(),
					timeoutManager.tookTime(), timeoutManager.isTimedOut() );
		}

		int nextChunkEndIndexInPage = nextChunkStartIndexInPage + chunkSize;

		LuceneLoadableSearchResult<H> loadableSearchResult;
		try {
			loadableSearchResult = currentPage.extract( nextChunkStartIndexInPage, nextChunkEndIndexInPage );
		}
		catch (IOException e) {
			throw log.ioExceptionOnQueryExecution( searcher.getLuceneQueryForExceptions(), e.getMessage(),
					EventContexts.fromIndexNames( scope.hibernateSearchIndexNames() ), e );
		}

		/*
		 * WARNING: the following call must run in the user thread.
		 * If we introduce async processing, we will have to add a nextAsync method here,
		 * as well as in ProjectionHitMapper and EntityLoader.
		 * This method may not be easy to implement for blocking mappers,
		 * so we may choose to throw exceptions for those.
		 */
		LuceneSearchResult<H> result = loadableSearchResult.loadBlocking();

		// increasing the index for further next(s)
		nextChunkOffset += chunkSize;
		return new LuceneSearchScrollResultImpl<>( currentPage.total(), true, result.hits(),
				result.took(), result.timedOut() );
	}

	private <T> T doSubmitWithIndexReader(ReadWork<T> work, HibernateSearchMultiReader indexReader) {
		return queryOrchestrator.submit(
				scope.hibernateSearchIndexNames(),
				scope.indexes(),
				routingKeys,
				work, indexReader
		);
	}
}
