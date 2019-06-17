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


public abstract class AbstractLuceneUpdateEntryWork extends AbstractLuceneWriteWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;

	private final String id;

	private final LuceneIndexEntry indexEntry;

	protected AbstractLuceneUpdateEntryWork(String indexName, String tenantId, String id, LuceneIndexEntry indexEntry) {
		super( "updateEntry", indexName );
		this.tenantId = tenantId;
		this.id = id;
		this.indexEntry = indexEntry;
	}

	@Override
	public Long execute(LuceneWriteWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			return doUpdateEntry( indexWriterDelegator, tenantId, id, indexEntry );
		}
		catch (IOException e) {
			throw log.unableToIndexEntry( tenantId, id, getEventContext(), e );
		}
	}

	protected abstract long doUpdateEntry(IndexWriterDelegator indexWriterDelegator, String tenantId, String id,
			LuceneIndexEntry indexEntry) throws IOException;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", entry=" ).append( indexEntry )
				.append( "]" );
		return sb.toString();
	}
}
