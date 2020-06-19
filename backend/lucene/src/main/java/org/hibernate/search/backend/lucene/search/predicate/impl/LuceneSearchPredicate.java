/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public interface LuceneSearchPredicate extends SearchPredicate {

	Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	Set<String> indexNames();

	void checkNestableWithin(String expectedParentNestedPath);

	Query toQuery(PredicateRequestContext context);

	static LuceneSearchPredicate from(LuceneSearchContext searchContext, SearchPredicate predicate) {
		if ( !( predicate instanceof LuceneSearchPredicate ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherPredicates( predicate );
		}
		LuceneSearchPredicate casted = (LuceneSearchPredicate) predicate;
		if ( !searchContext.indexes().indexNames().equals( casted.indexNames() ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.indexNames(),
					searchContext.indexes().indexNames() );
		}
		return casted;
	}

}
