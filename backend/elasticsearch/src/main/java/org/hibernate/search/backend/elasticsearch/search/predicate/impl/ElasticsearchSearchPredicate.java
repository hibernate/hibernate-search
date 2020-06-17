/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchSearchPredicate implements SearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// TODO HSEARCH-3476 this is just a temporary hack:
	//  we should move to one SearchPredicate implementation per type of predicate.
	public static ElasticsearchSearchPredicate of(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchPredicateBuilder builder) {
		return new ElasticsearchSearchPredicate( searchContext.indexes().hibernateSearchIndexNames(), builder );
	}

	public static ElasticsearchSearchPredicate from(ElasticsearchSearchContext searchContext, SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}
		ElasticsearchSearchPredicate casted = (ElasticsearchSearchPredicate) predicate;
		if ( !searchContext.indexes().hibernateSearchIndexNames().equals( casted.indexNames ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.indexNames,
					searchContext.indexes().hibernateSearchIndexNames() );
		}
		return casted;
	}

	private final Set<String> indexNames;
	private final ElasticsearchSearchPredicateBuilder delegate;

	ElasticsearchSearchPredicate(Set<String> indexNames, ElasticsearchSearchPredicateBuilder delegate) {
		this.indexNames = indexNames;
		this.delegate = delegate;
	}

	public void checkNestableWithin(String expectedParentNestedPath) {
		delegate.checkNestableWithin( expectedParentNestedPath );
	}

	public JsonObject toJsonQuery(PredicateRequestContext context) {
		return delegate.toJsonQuery( context );
	}

}
