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
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;


public class LuceneDeleteEntryWork extends AbstractLuceneWriteWork<Long>
		implements LuceneSingleDocumentWriteWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String tenantId;

	private final String id;

	private final Query filter;

	LuceneDeleteEntryWork(String tenantId, String id, Query filter) {
		super( "deleteEntry" );
		this.tenantId = tenantId;
		this.id = id;
		this.filter = filter;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", tenantId=" ).append( tenantId )
				.append( ", id=" ).append( id )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public Long execute(LuceneWriteWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			Term idTerm = new Term( MetadataFields.idFieldName(), id );
			if ( filter == null ) {
				// Pass the term directly instead of a query: presumably more efficient.
				return indexWriterDelegator.deleteDocuments( idTerm );
			}
			else {
				return indexWriterDelegator.deleteDocuments( Queries.boolFilter( new TermQuery( idTerm ), filter ) );
			}
		}
		catch (IOException e) {
			throw log.unableToDeleteEntryFromIndex( tenantId, id, context.getEventContext(), e );
		}
	}

	@Override
	public String getDocumentId() {
		return id;
	}
}
