/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchNestedPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonAccessor<String> PATH_ACCESSOR = JsonAccessor.root().property( "path" ).asString();
	private static final JsonAccessor<JsonObject> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asObject();
	private static final JsonAccessor<Boolean> IGNORE_UNMAPPED_ACCESSOR =
			JsonAccessor.root().property( "ignore_unmapped" ).asBoolean();

	private final ElasticsearchSearchPredicate nestedPredicate;

	private ElasticsearchNestedPredicate(Builder builder) {
		super( builder );
		nestedPredicate = builder.nestedPredicate;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		PredicateRequestContext nestedContext = context.withNestedPath( absoluteFieldPath );

		wrap( indexNames(), absoluteFieldPath, outerObject, innerObject,
				nestedPredicate.toJsonQuery( nestedContext )
		);

		return outerObject;
	}

	static void wrap(Set<String> indexNames, String absoluteFieldPath,
			JsonObject outerObject, JsonObject innerObject, JsonObject toWrap) {
		PATH_ACCESSOR.set( innerObject, absoluteFieldPath );
		QUERY_ACCESSOR.set( innerObject, toWrap );
		if ( indexNames.size() > 1 ) {
			// There are multiple target indexes; some of them may not declare the nested field.
			// Instruct ES to behave as if the nested field had no value in that case.
			IGNORE_UNMAPPED_ACCESSOR.set( innerObject, true );
		}
		outerObject.add( "nested", innerObject );
	}

	public static class Factory
			extends AbstractElasticsearchCompositeNodeSearchQueryElementFactory<NestedPredicateBuilder> {
		@Override
		public NestedPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			return new Builder( scope, node );
		}
	}

	private static class Builder extends AbstractBuilder implements NestedPredicateBuilder {
		private ElasticsearchSearchPredicate nestedPredicate;

		Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexCompositeNodeContext field) {
			super( scope, field.absolutePath(),
					// nestedPathHierarchy includes absoluteFieldPath at the end, but here we don't want it to be included.
					field.nestedPathHierarchy().subList( 0, field.nestedPathHierarchy().size() - 1 ) );
		}

		@Override
		public void nested(SearchPredicate nestedPredicate) {
			ElasticsearchSearchPredicate elasticsearchPredicate = ElasticsearchSearchPredicate.from(
					scope, nestedPredicate );
			elasticsearchPredicate.checkNestableWithin( absoluteFieldPath );
			this.nestedPredicate = elasticsearchPredicate;
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchNestedPredicate( this );
		}
	}
}
