/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchTextRegexpPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor REGEXP_ACCESSOR = JsonAccessor.root().property( "regexp" ).asObject();

	private static final JsonAccessor<JsonElement> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );

	private final JsonPrimitive pattern;

	public ElasticsearchTextRegexpPredicate(Builder builder) {
		super( builder );
		this.pattern = builder.pattern;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		VALUE_ACCESSOR.set( innerObject, pattern );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		REGEXP_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory
			extends AbstractElasticsearchSearchValueFieldQueryElementFactory<RegexpPredicateBuilder, String> {
		@Override
		public RegexpPredicateBuilder create(ElasticsearchSearchIndexScope scope,
				ElasticsearchSearchValueFieldContext<String> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements RegexpPredicateBuilder {
		private JsonPrimitive pattern;

		private Builder(ElasticsearchSearchIndexScope scope, ElasticsearchSearchValueFieldContext<String> field) {
			super( scope, field );
		}

		@Override
		public void pattern(String pattern) {
			this.pattern = new JsonPrimitive( pattern );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchTextRegexpPredicate( this );
		}
	}
}
