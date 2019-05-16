/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneDeleteEntryWork extends AbstractLuceneWriteWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;

	private final String id;

	public AbstractLuceneDeleteEntryWork(String indexName, String tenantId, String id) {
		super( "deleteEntry", indexName );
		this.tenantId = tenantId;
		this.id = id;
	}

	@Override
	public Long execute(LuceneWriteWorkExecutionContext context) {
		try {
			IndexWriter indexWriter = context.getIndexWriter();
			return doDeleteDocuments( indexWriter, tenantId, id );
		}
		catch (IOException e) {
			throw log.unableToDeleteEntryFromIndex( tenantId, id, getEventContext(), e );
		}
	}

	protected abstract long doDeleteDocuments(IndexWriter indexWriter, String tenantId, String id) throws IOException;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", indexName=" ).append( indexName )
				.append( ", id=" ).append( id )
				.append( "]" );
		return sb.toString();
	}
}
