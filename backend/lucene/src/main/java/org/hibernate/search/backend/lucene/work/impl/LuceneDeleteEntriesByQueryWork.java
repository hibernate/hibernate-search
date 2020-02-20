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

import org.apache.lucene.search.Query;

public class LuceneDeleteEntriesByQueryWork extends AbstractLuceneWriteWork<Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Query query;

	LuceneDeleteEntriesByQueryWork(Query query) {
		super( "deleteByQuery" );
		this.query = query;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( "query=" ).append( query )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public Long execute(LuceneWriteWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			return indexWriterDelegator.deleteDocuments( query );
		}
		catch (IOException e) {
			throw log.unableToDeleteAllEntriesFromIndex( query, context.getEventContext(), e );
		}
	}

}
