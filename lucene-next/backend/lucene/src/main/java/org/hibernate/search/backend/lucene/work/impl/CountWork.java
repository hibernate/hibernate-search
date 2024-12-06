/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;

import org.apache.lucene.search.IndexSearcher;

public class CountWork implements ReadWork<Integer> {

	private final LuceneSearcher<?, ?> searcher;

	CountWork(LuceneSearcher<?, ?> searcher) {
		this.searcher = searcher;
	}

	@Override
	public Integer execute(ReadWorkExecutionContext context) {
		try {
			IndexSearcher indexSearcher = context.createSearcher();

			return searcher.count( indexSearcher );
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
				.append( "]" );
		return sb.toString();
	}
}
