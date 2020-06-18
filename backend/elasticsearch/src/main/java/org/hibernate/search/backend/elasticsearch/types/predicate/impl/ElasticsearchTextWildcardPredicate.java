/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchTextWildcardPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor WILDCARD_ACCESSOR = JsonAccessor.root().property( "wildcard" ).asObject();

	private static final JsonAccessor<JsonElement> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );

	private final JsonPrimitive pattern;

	public ElasticsearchTextWildcardPredicate(Builder builder) {
		super( builder );
		this.pattern = builder.pattern;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		VALUE_ACCESSOR.set( innerObject, pattern );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		WILDCARD_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Builder extends AbstractBuilder implements WildcardPredicateBuilder {
		private JsonPrimitive pattern;

		public Builder(ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<String> field) {
			super( searchContext, field );
		}

		@Override
		public void pattern(String pattern) {
			this.pattern = new JsonPrimitive( pattern );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchTextWildcardPredicate( this );
		}
	}
}
