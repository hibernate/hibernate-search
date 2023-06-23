/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchExistsPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor EXISTS_ACCESSOR = JsonAccessor.root().property( "exists" ).asObject();
	private static final JsonAccessor<String> FIELD_ACCESSOR = JsonAccessor.root().property( "field" ).asString();

	private ElasticsearchExistsPredicate(Builder builder) {
		super( builder );
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		FIELD_ACCESSOR.set( innerObject, absoluteFieldPath );

		EXISTS_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	public static class Factory<F>
			extends AbstractElasticsearchValueFieldSearchQueryElementFactory<ExistsPredicateBuilder, F> {
		@Override
		public ExistsPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Builder( scope, field );
		}
	}

	public static class ObjectFieldFactory
			extends AbstractElasticsearchCompositeNodeSearchQueryElementFactory<ExistsPredicateBuilder> {
		@Override
		public ExistsPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			return new Builder( scope, node );
		}
	}

	private static class Builder extends AbstractBuilder implements ExistsPredicateBuilder {
		Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexNodeContext node) {
			super( scope, node );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchExistsPredicate( this );
		}
	}
}
