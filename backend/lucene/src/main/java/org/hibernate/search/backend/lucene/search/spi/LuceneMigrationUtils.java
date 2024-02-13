/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSort;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.sort.SearchSort;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

public final class LuceneMigrationUtils {

	private LuceneMigrationUtils() {
	}

	public static Query toLuceneQuery(SearchPredicate predicate) {
		return ( (LuceneSearchPredicate) predicate ).toQuery( PredicateRequestContext.root() );
	}

	public static Sort toLuceneSort(SearchSort sort) {
		List<SortField> result = new ArrayList<>();
		LuceneSearchSortCollector collector = new LuceneSearchSortCollector() {
			@Override
			public void collectSortField(SortField sortField) {
				result.add( sortField );
			}

			@Override
			public void collectSortFields(SortField[] sortFields) {
				Collections.addAll( result, sortFields );
			}
		};
		( (LuceneSearchSort) sort ).toSortFields( collector );
		return new Sort( result.toArray( new SortField[0] ) );
	}
}
