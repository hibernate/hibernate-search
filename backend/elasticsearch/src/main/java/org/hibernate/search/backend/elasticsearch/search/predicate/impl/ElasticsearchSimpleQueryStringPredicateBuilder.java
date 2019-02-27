/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchSimpleQueryStringPredicateBuilderFieldContext;
import org.hibernate.search.engine.search.predicate.spi.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchSimpleQueryStringPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder
		implements SimpleQueryStringPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor SIMPLE_QUERY_STRING_ACCESSOR = JsonAccessor.root().property( "simple_query_string" ).asObject();
	private static final JsonAccessor<String> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asString();
	private static final JsonAccessor<JsonElement> DEFAULT_OPERATOR_ACCESSOR = JsonAccessor.root().property( "default_operator" );
	private static final JsonAccessor<JsonArray> FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asArray();


	private static final JsonPrimitive AND_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "and" );
	private static final JsonPrimitive OR_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "or" );


	private final ElasticsearchSearchTargetModel searchTargetModel;

	private final Map<String, ElasticsearchSimpleQueryStringPredicateBuilderFieldContext> fields = new LinkedHashMap<>();
	private JsonPrimitive defaultOperator = OR_OPERATOR_KEYWORD_JSON;
	private String simpleQueryString;

	ElasticsearchSimpleQueryStringPredicateBuilder(ElasticsearchSearchTargetModel searchTargetModel) {
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public void withAndAsDefaultOperator() {
		this.defaultOperator = AND_OPERATOR_KEYWORD_JSON;
	}

	@Override
	public FieldContext field(String absoluteFieldPath) {
		ElasticsearchSimpleQueryStringPredicateBuilderFieldContext field = fields.get( absoluteFieldPath );
		if ( field == null ) {
			field = searchTargetModel.getSchemaNodeComponent(
					absoluteFieldPath,
					ElasticsearchSearchPredicateBuilderFactoryImpl.PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY,
					DslConverter.DISABLED
			)
					.createSimpleQueryStringFieldContext( absoluteFieldPath );
			fields.put( absoluteFieldPath, field );
		}
		return field;
	}

	@Override
	public void simpleQueryString(String simpleQueryString) {
		this.simpleQueryString = simpleQueryString;
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		QUERY_ACCESSOR.set( innerObject, simpleQueryString );
		DEFAULT_OPERATOR_ACCESSOR.set( innerObject, defaultOperator );

		JsonArray fieldArray = new JsonArray();
		for ( ElasticsearchSimpleQueryStringPredicateBuilderFieldContext fieldContext : fields.values() ) {
			fieldArray.add( fieldContext.build() );
		}
		FIELDS_ACCESSOR.set( innerObject, fieldArray );

		SIMPLE_QUERY_STRING_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

}
