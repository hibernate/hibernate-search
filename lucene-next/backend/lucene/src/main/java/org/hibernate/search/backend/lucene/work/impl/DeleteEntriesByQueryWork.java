/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;

import org.apache.lucene.search.Query;

public class DeleteEntriesByQueryWork implements IndexManagementWork<Long> {

	private final Query query;

	DeleteEntriesByQueryWork(Query query) {
		this.query = query;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "query=" ).append( query )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public Long execute(IndexManagementWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexAccessor().getIndexWriterDelegator();
			return indexWriterDelegator.deleteDocuments( query );
		}
		catch (IOException e) {
			throw QueryLog.INSTANCE.unableToDeleteAllEntriesFromIndex( query, e.getMessage(), context.getEventContext(), e );
		}
	}

	@Override
	public Object getInfo() {
		return this;
	}
}
