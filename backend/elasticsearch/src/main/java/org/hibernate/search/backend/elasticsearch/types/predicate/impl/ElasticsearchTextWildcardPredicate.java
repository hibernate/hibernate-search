/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchTextWildcardPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor WILDCARD_ACCESSOR = JsonAccessor.root().property( "wildcard" ).asObject();

	private static final JsonAccessor<JsonElement> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );

	private final QueryParametersValueProvider<JsonPrimitive> patternProvider;

	private ElasticsearchTextWildcardPredicate(Builder builder) {
		super( builder );
		this.patternProvider = builder.patternProvider;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		VALUE_ACCESSOR.set( innerObject, patternProvider.provide( context.toQueryParametersContext() ) );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		WILDCARD_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory
			extends AbstractElasticsearchValueFieldSearchQueryElementFactory<WildcardPredicateBuilder, String> {
		@Override
		public WildcardPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements WildcardPredicateBuilder {
		private QueryParametersValueProvider<JsonPrimitive> patternProvider;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<String> field) {
			super( scope, field );
		}

		@Override
		public void pattern(String pattern) {
			this.patternProvider = simple( new JsonPrimitive( pattern ) );
		}

		@Override
		public void param(String parameterName) {
			this.patternProvider = parameter( parameterName, String.class, JsonPrimitive::new );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchTextWildcardPredicate( this );
		}
	}
}
