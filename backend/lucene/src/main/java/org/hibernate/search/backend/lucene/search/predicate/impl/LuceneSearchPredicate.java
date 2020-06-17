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

public class LuceneSearchPredicate implements SearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// TODO HSEARCH-3476 this is just a temporary hack:
	//  we should move to one SearchPredicate implementation per type of predicate.
	public static LuceneSearchPredicate of(LuceneSearchContext searchContext, LuceneSearchPredicateBuilder builder) {
		return new LuceneSearchPredicate( searchContext.indexes().indexNames(), builder );
	}

	public static LuceneSearchPredicate from(LuceneSearchContext searchContext, SearchPredicate predicate) {
		if ( !( predicate instanceof LuceneSearchPredicate ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherPredicates( predicate );
		}
		LuceneSearchPredicate casted = (LuceneSearchPredicate) predicate;
		if ( !searchContext.indexes().indexNames().equals( casted.indexNames ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.indexNames, searchContext.indexes().indexNames() );
		}
		return casted;
	}

	private final Set<String> indexNames;
	private final LuceneSearchPredicateBuilder delegate;

	private LuceneSearchPredicate(Set<String> indexNames, LuceneSearchPredicateBuilder delegate) {
		this.indexNames = indexNames;
		this.delegate = delegate;
	}

	public void checkNestableWithin(String expectedParentNestedPath) {
		delegate.checkNestableWithin( expectedParentNestedPath );
	}

	public Query toQuery(PredicateRequestContext context) {
		return delegate.toQuery( context );
	}

}
