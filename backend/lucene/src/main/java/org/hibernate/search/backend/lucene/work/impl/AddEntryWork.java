/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class AddEntryWork extends AbstractSingleDocumentIndexingWork {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
			throw log.unableToIndexEntry(
					tenantId, entityTypeName, entityIdentifier, e.getMessage(), context.getEventContext(), e
			);
		}
	}

}
