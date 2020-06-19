/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;

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
	private final List<String> values;

	private ElasticsearchMatchIdPredicate(Builder builder) {
		super( builder );
		documentIdHelper = builder.searchContext.documentIdHelper();
		values = builder.values;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.values = null;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		JsonArray array = toJsonArray( values, context.getTenantId() );

		VALUES_ACCESSOR.set( innerObject, array );

		IDS_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	private JsonArray toJsonArray(List<String> list, String tenantId) {
		JsonArray jsonArray = new JsonArray( list.size() );
		for ( String value : list ) {
			jsonArray.add( documentIdHelper.toElasticsearchId( tenantId, value ) );
		}
		return jsonArray;
	}

	static class Builder extends AbstractElasticsearchPredicate.AbstractBuilder implements MatchIdPredicateBuilder {

		private List<String> values = new ArrayList<>();

		Builder(ElasticsearchSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public void value(Object value, ValueConvert valueConvert) {
			ToDocumentIdentifierValueConverter<?> dslToDocumentIdConverter =
					searchContext.indexes().idDslConverter( valueConvert );
			ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext =
					searchContext.toDocumentIdentifierValueConvertContext();
			values.add( dslToDocumentIdConverter.convertUnknown( value, toDocumentIdentifierValueConvertContext ) );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchMatchIdPredicate( this );
		}
	}
}
