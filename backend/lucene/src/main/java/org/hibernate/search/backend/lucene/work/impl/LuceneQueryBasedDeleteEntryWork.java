/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;


public class LuceneQueryBasedDeleteEntryWork extends AbstractLuceneDeleteEntryWork {

	public LuceneQueryBasedDeleteEntryWork(String tenantId, String id) {
		super( tenantId, id );
	}

	@Override
	protected long doDeleteDocuments(IndexWriterDelegator indexWriterDelegator, String tenantId, String id) throws IOException {
		return indexWriterDelegator.deleteDocuments(
				LuceneQueries.discriminatorMultiTenancyDeleteDocumentQuery( tenantId, id )
		);
	}
}
