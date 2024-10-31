/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;

import org.apache.lucene.search.IndexSearcher;

public class SearchWork<R> implements ReadWork<R> {

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
			throw QueryLog.INSTANCE.ioExceptionOnQueryExecution( searcher.getLuceneQueryForExceptions(), e.getMessage(),
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
