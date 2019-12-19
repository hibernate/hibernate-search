/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;


public class LuceneQueryBasedUpdateEntryWork extends AbstractLuceneUpdateEntryWork {

	public LuceneQueryBasedUpdateEntryWork(String tenantId, String id, LuceneIndexEntry indexEntry) {
		super( tenantId, id, indexEntry );
	}

	@Override
	protected long doUpdateEntry(IndexWriterDelegator indexWriterDelegator, String tenantId, String id,
			LuceneIndexEntry indexEntry) throws IOException {
		indexWriterDelegator.deleteDocuments( Queries.singleDocumentQuery( tenantId, id ) );
		return indexWriterDelegator.addDocuments( indexEntry );
	}
}
