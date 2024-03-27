/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.IndexSearcher;

public class ScrollWork<ER> implements ReadWork<ER> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearcher<?, ER> searcher;

	private final int offset;
	private final int limit;
	private final int totalHitCountThreshold;

	ScrollWork(LuceneSearcher<?, ER> searcher, int offset, int limit, int totalHitCountThreshold) {
		this.offset = offset;
		this.limit = limit;
		this.searcher = searcher;
		this.totalHitCountThreshold = totalHitCountThreshold;
	}

	@Override
	public ER execute(ReadWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = context.createSearcher();

			return searcher.scroll( indexSearcher, context.getIndexReaderMetadataResolver(), offset, limit,
					totalHitCountThreshold );
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
				.append( ", limit=" ).append( limit )
				.append( "]" );
		return sb.toString();
	}
}
