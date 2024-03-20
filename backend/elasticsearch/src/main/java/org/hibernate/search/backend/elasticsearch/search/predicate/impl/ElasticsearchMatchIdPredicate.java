/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Generate the JSON for queries by id for Elasticsearch.
 * <p>
 * Example:
 * <pre>
 * {@code
 * GET /_search
 * {
 *     "query": {
 *         "ids" : {
 *             "values" : ["1", "4", "100"]
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * @author Davide D'Alto
 */
public class ElasticsearchMatchIdPredicate extends AbstractElasticsearchPredicate {

	private static final JsonObjectAccessor IDS_ACCESSOR = JsonAccessor.root().property( "ids" ).asObject();
	private static final JsonAccessor<JsonElement> VALUES_ACCESSOR = JsonAccessor.root().property( "values" );

	private final DocumentIdHelper documentIdHelper;
	private final List<QueryParametersValueProvider<String>> valueProviders;

	private ElasticsearchMatchIdPredicate(Builder builder) {
		super( builder );
		documentIdHelper = builder.scope.documentIdHelper();
		valueProviders = builder.valueProviders;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.valueProviders = null;
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		// Nothing to do
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		JsonArray array = toJsonArray( valueProviders, context );

		VALUES_ACCESSOR.set( innerObject, array );

		IDS_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	private JsonArray toJsonArray(List<QueryParametersValueProvider<String>> valueProviders, PredicateRequestContext context) {
		JsonArray jsonArray = new JsonArray( valueProviders.size() );
		for ( QueryParametersValueProvider<String> provider : valueProviders ) {
			jsonArray.add( documentIdHelper.toElasticsearchId( context.getTenantId(), provider.provide( context ) ) );
		}
		return jsonArray;
	}

	static class Builder extends AbstractElasticsearchPredicate.AbstractBuilder implements MatchIdPredicateBuilder {

		private List<QueryParametersValueProvider<String>> valueProviders = new ArrayList<>();

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void value(Object value, ValueConvert valueConvert) {
			DslConverter<?, String> converter = scope.identifier().dslConverter( valueConvert );
			ToDocumentValueConvertContext context = scope.toDocumentValueConvertContext();
			valueProviders.add( simple( converter.unknownTypeToDocumentValue( value, context ) ) );
		}

		@Override
		public void param(String parameterName, ValueConvert valueConvert) {
			DslConverter<?, String> converter = scope.identifier().dslConverter( valueConvert );
			ToDocumentValueConvertContext context = scope.toDocumentValueConvertContext();
			valueProviders.add(
					parameter( parameterName, String.class, value -> converter.unknownTypeToDocumentValue( value, context ) ) );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchMatchIdPredicate( this );
		}
	}
}
