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


public class LuceneMergeSegmentsWork extends AbstractLuceneWriteWork<Void> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	LuceneMergeSegmentsWork() {
		super( "mergeSegmentsIndexSegments" );
	}

	@Override
	public Void execute(LuceneWriteWorkExecutionContext context) {
		try {
			IndexWriterDelegator indexWriterDelegator = context.getIndexWriterDelegator();
			indexWriterDelegator.mergeSegments();
			return null;
		}
		catch (IOException e) {
			throw log.unableToMergeSegments( context.getEventContext(), e );
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( workType )
				.append( "]" );
		return sb.toString();
	}
}