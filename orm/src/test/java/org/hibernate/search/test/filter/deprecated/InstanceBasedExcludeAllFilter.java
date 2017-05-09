/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter.deprecated;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.impl.AndDocIdSet;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class InstanceBasedExcludeAllFilter extends Filter implements Serializable {

	private static volatile int constructorCount = 0;

	public InstanceBasedExcludeAllFilter() {
		constructorCount++;
	}

	@Override
	public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs) throws IOException {
		LeafReader reader = context.reader();
		ExcludeAllFilter.verifyItsAReadOnlySegmentReader( reader );
		return AndDocIdSet.EMPTY_DOCIDSET;
	}

	public static void reset() {
		constructorCount = 0;
	}

	public static void assertConstructorInvoked(int times) {
		if ( constructorCount != times ) {
			throw new SearchException( "test failed, constructor invoked " + constructorCount + ", expected " + times );
		}
	}

	@Override
	public String toString(String field) {
		return "";
	}
}
