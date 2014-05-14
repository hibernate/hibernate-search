/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentReader;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.impl.AndDocIdSet;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ExcludeAllFilter extends Filter implements Serializable {

	// ugly but useful for test purposes
	private static final Map<IndexReader,IndexReader> invokedOnReaders = new ConcurrentHashMap<IndexReader,IndexReader>();

	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		AtomicReader reader = context.reader();
		verifyItsAReadOnlySegmentReader( reader );
		final IndexReader previousValue = invokedOnReaders.put( reader, reader );
		if ( previousValue != null ) {
			throw new IllegalStateException( "Called twice" );
		}
		return AndDocIdSet.EMPTY_DOCIDSET;
	}

	public static void verifyItsAReadOnlySegmentReader(IndexReader reader) {
		if ( ! ( reader instanceof SegmentReader ) ) {
			throw new SearchException( "test failed: we should receive subreaders" );
		}
	}

}
