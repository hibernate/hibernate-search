/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.exception.SearchException;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class InstanceBasedExcludeAllFilterFactory {

	private static volatile int creationCount = 0;

	@Factory
	public Query create() {
		creationCount++;
		return new MatchNoDocsQuery();
	}

	public static void reset() {
		creationCount = 0;
	}

	public static void assertInstancesCreated(int count) {
		if ( creationCount != count ) {
			throw new SearchException( "test failed, " + creationCount + " instances were created, expected " + count );
		}
	}
}
