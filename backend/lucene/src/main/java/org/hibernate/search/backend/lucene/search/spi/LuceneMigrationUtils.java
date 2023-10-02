/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		// TODO: this util is for v5 predicates so we won't get a knn predicate here ...
		//  and if we do, there will be an exception that user would need to report back so we can see what's their use case is...
		return ( (LuceneSearchPredicate) predicate ).toQuery( PredicateRequestContext.withoutSession() );
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

			@Override
			public PredicateRequestContext toPredicateRequestContext(String absoluteNestedPath) {
				// and if someone tries to access filter methods the sessionless context will take care of throwing an exception.
				return PredicateRequestContext.withoutSession().withNestedPath( absoluteNestedPath );
			}
		};
		( (LuceneSearchSort) sort ).toSortFields( collector );
		return new Sort( result.toArray( new SortField[0] ) );
	}
}
