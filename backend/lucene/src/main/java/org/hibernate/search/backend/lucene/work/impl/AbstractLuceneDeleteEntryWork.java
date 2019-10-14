/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public abstract class AbstractLuceneDeleteEntryWork extends AbstractLuceneWriteWork<Long>
		implements LuceneSingleDocumentWriteWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;

	private final String id;

	AbstractLuceneDeleteEntryWork(String tenantId, String id) {
		super( "deleteEntry" );
		this.tenantId = tenantId;
		this.id = id;
	}

	@Override
	public Long execute(LuceneWriteWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			return doDeleteDocuments( indexWriterDelegator, tenantId, id );
		}
		catch (IOException e) {
			throw log.unableToDeleteEntryFromIndex( tenantId, id, context.getEventContext(), e );
		}
	}

	protected abstract long doDeleteDocuments(IndexWriterDelegator indexWriterDelegator, String tenantId, String id)
			throws IOException;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", id=" ).append( id )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public String getDocumentId() {
		return id;
	}
}
