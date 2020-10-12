/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchSearchObjectFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchObjectFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchValueFieldContext;
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
			extends AbstractElasticsearchSearchValueFieldQueryElementFactory<ExistsPredicateBuilder, F> {
		@Override
		public ExistsPredicateBuilder create(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchValueFieldContext<F> field) {
			return new Builder( searchContext, field.absolutePath(), field.nestedPathHierarchy() );
		}
	}

	public static class ObjectFieldFactory
			extends AbstractElasticsearchSearchObjectFieldQueryElementFactory<ExistsPredicateBuilder> {
		public static final ObjectFieldFactory INSTANCE = new ObjectFieldFactory();

		private ObjectFieldFactory() {
		}

		@Override
		public ExistsPredicateBuilder create(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchObjectFieldContext field) {
			return new Builder( searchContext, field.absolutePath(), field.nestedPathHierarchy() );
		}
	}

	public static class Builder extends AbstractBuilder implements ExistsPredicateBuilder {
		public Builder(ElasticsearchSearchContext searchContext, String absoluteFieldPath,
				List<String> nestedPathHierarchy) {
			super( searchContext, absoluteFieldPath, nestedPathHierarchy );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchExistsPredicate( this );
		}
	}
}
