/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;


public class LuceneTermBasedUpdateEntryWork extends AbstractLuceneUpdateEntryWork {

	public LuceneTermBasedUpdateEntryWork(String tenantId, String id, LuceneIndexEntry indexEntry) {
		super( tenantId, id, indexEntry );
	}

	@Override
	protected long doUpdateEntry(IndexWriterDelegator indexWriterDelegator, String tenantId, String id,
			LuceneIndexEntry indexEntry) throws IOException {
		return indexWriterDelegator.updateDocuments( new Term( LuceneFields.idFieldName(), id ), indexEntry );
	}
}
