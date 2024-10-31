/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.logging.impl.IndexingLog;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;

public class AddEntryWork extends AbstractSingleDocumentIndexingWork {

	private final LuceneIndexEntry indexEntry;

	AddEntryWork(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, LuceneIndexEntry indexEntry) {
		super( "addEntry", tenantId, entityTypeName, entityIdentifier, documentIdentifier );
		this.indexEntry = indexEntry;
	}

	@Override
	public Long execute(IndexingWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			return indexWriterDelegator.addDocuments( indexEntry );
		}
		catch (IOException e) {
			throw IndexingLog.INSTANCE.unableToIndexEntry(
					tenantId, entityTypeName, entityIdentifier, e.getMessage(), context.getEventContext(), e
			);
		}
	}

}
