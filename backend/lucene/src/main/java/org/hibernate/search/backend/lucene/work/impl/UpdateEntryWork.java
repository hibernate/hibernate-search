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
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class UpdateEntryWork extends AbstractSingleDocumentIndexingWork {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Query filter;

	private final LuceneIndexEntry indexEntry;

	UpdateEntryWork(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, Query filter, LuceneIndexEntry indexEntry) {
		super( "updateEntry", tenantId, entityTypeName, entityIdentifier, documentIdentifier );
		this.filter = filter;
		this.indexEntry = indexEntry;
	}

	@Override
	public Long execute(IndexingWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			Term idTerm = new Term( MetadataFields.idFieldName(), documentIdentifier );
			if ( filter == null ) {
				// Atomic update: presumably more efficient.
				return indexWriterDelegator.updateDocuments( idTerm, indexEntry );
			}
			else {
				indexWriterDelegator.deleteDocuments( Queries.boolFilter( new TermQuery( idTerm ), filter ) );
				return indexWriterDelegator.addDocuments( indexEntry );
			}
		}
		catch (IOException e) {
			throw log.unableToIndexEntry( tenantId, entityTypeName, entityIdentifier, e.getMessage(), context.getEventContext(),
					e );
		}
	}

}
