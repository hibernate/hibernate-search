/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.spi;

import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import org.apache.lucene.search.Query;

public final class LuceneMigrationUtils {

	private LuceneMigrationUtils() {
	}

	public static Query toLuceneQuery(SearchPredicate predicate) {
		return ( (LuceneSearchPredicate) predicate ).toQuery( PredicateRequestContext.root() );
	}

}
