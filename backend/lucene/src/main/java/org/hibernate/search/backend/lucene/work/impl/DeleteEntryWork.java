/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class DeleteEntryWork extends AbstractSingleDocumentIndexingWork {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Query filter;

	DeleteEntryWork(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, Query filter) {
		super( "deleteEntry", tenantId, entityTypeName, entityIdentifier, documentIdentifier );
		this.filter = filter;
	}

	@Override
	public Long execute(IndexingWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			Term idTerm = new Term( MetadataFields.idFieldName(), documentIdentifier );
			if ( filter == null ) {
				// Pass the term directly instead of a query: presumably more efficient.
				return indexWriterDelegator.deleteDocuments( idTerm );
			}
			else {
				return indexWriterDelegator.deleteDocuments( Queries.boolFilter( new TermQuery( idTerm ), filter ) );
			}
		}
		catch (IOException e) {
			throw log.unableToDeleteEntryFromIndex(
					tenantId, entityTypeName, entityIdentifier, e.getMessage(), context.getEventContext(), e
			);
		}
	}

}
