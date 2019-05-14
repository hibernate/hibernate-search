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
public class LuceneFlushWork extends AbstractLuceneWriteWork<Void> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public LuceneFlushWork(String indexName) {
		super( "flushIndex", indexName );
	}

	@Override
	public Void execute(LuceneWriteWorkExecutionContext context) {
		IndexWriter indexWriter = context.getIndexWriter();
		try {
			indexWriter.flush();
			return null;
		}
		catch (IOException e) {
			throw log.unableToFlushIndex( getEventContext(), e );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( ", indexName=" ).append( indexName )
				.append( "]" );
		return sb.toString();
	}
}
