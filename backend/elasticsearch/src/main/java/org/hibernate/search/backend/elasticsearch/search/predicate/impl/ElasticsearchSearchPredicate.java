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
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public abstract class ElasticsearchSearchPredicate implements SearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public abstract Set<String> indexNames();

	public abstract void checkNestableWithin(String expectedParentNestedPath);

	public final JsonObject toJsonQuery(PredicateRequestContext context) {
		return buildJsonQuery( context.predicateContext( this ) );
	}

	public abstract JsonObject buildJsonQuery(PredicateRequestContext context);

	public static ElasticsearchSearchPredicate from(ElasticsearchSearchIndexScope<?> scope, SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}
		ElasticsearchSearchPredicate casted = (ElasticsearchSearchPredicate) predicate;
		if ( !scope.hibernateSearchIndexNames().equals( casted.indexNames() ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.indexNames(),
					scope.hibernateSearchIndexNames() );
		}
		return casted;
	}
}
