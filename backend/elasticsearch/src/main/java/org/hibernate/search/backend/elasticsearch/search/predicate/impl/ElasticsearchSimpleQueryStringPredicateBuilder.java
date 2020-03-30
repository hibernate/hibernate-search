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
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchDifferentNestedObjectCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopedIndexFieldComponent;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchSucceedingCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerConstants;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.EnumSet;
import java.util.Set;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.util.common.AssertionFailure;

public class ElasticsearchSimpleQueryStringPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder
		implements SimpleQueryStringPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final JsonObjectAccessor SIMPLE_QUERY_STRING_ACCESSOR = JsonAccessor.root().property( "simple_query_string" ).asObject();
	private static final JsonAccessor<String> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asString();
	private static final JsonAccessor<JsonElement> DEFAULT_OPERATOR_ACCESSOR = JsonAccessor.root().property( "default_operator" );
	private static final JsonAccessor<JsonArray> FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asArray();
	private static final JsonAccessor<String> ANALYZER_ACCESSOR = JsonAccessor.root().property( "analyzer" ).asString();
	private static final JsonAccessor<String> FLAGS_ACCESSOR = JsonAccessor.root().property( "flags" ).asString();

	private static final JsonPrimitive AND_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "and" );
	private static final JsonPrimitive OR_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "or" );

	private final ElasticsearchScopeModel scopeModel;

	private final Map<String, ElasticsearchSimpleQueryStringPredicateBuilderFieldState> fields = new LinkedHashMap<>();
	private JsonPrimitive defaultOperator = OR_OPERATOR_KEYWORD_JSON;
	private String simpleQueryString;
	private String analyzer;
	private EnumSet<SimpleQueryFlag> flags;
	private ElasticsearchCompatibilityChecker analyzerChecker = new ElasticsearchSucceedingCompatibilityChecker();
	private ElasticsearchDifferentNestedObjectCompatibilityChecker nestedCompatibilityChecker;

	ElasticsearchSimpleQueryStringPredicateBuilder(ElasticsearchScopeModel scopeModel) {
		this.scopeModel = scopeModel;
		this.nestedCompatibilityChecker = ElasticsearchDifferentNestedObjectCompatibilityChecker.empty( scopeModel );
	}

	@Override
	public void defaultOperator(BooleanOperator operator) {
		switch ( operator ) {
			case AND:
				this.defaultOperator = AND_OPERATOR_KEYWORD_JSON;
				break;
			case OR:
				this.defaultOperator = OR_OPERATOR_KEYWORD_JSON;
				break;
		}
	}

	@Override
	public void flags(Set<SimpleQueryFlag> flags) {
		this.flags = EnumSet.copyOf( flags );
	}

	@Override
	public FieldState field(String absoluteFieldPath) {
		ElasticsearchSimpleQueryStringPredicateBuilderFieldState field = fields.get( absoluteFieldPath );
		if ( field == null ) {
			ElasticsearchScopedIndexFieldComponent<ElasticsearchFieldPredicateBuilderFactory> fieldComponent = scopeModel.getSchemaNodeComponent(
					absoluteFieldPath, ElasticsearchSearchPredicateBuilderFactoryImpl.PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
			field = fieldComponent.getComponent().createSimpleQueryStringFieldContext( absoluteFieldPath );
			analyzerChecker = analyzerChecker.combine( fieldComponent.getAnalyzerCompatibilityChecker() );
			nestedCompatibilityChecker = nestedCompatibilityChecker.combineAndCheck( absoluteFieldPath );
			fields.put( absoluteFieldPath, field );
		}
		return field;
	}

	@Override
	public void simpleQueryString(String simpleQueryString) {
		this.simpleQueryString = simpleQueryString;
	}

	@Override
	public void analyzer(String analyzerName) {
		this.analyzer = analyzerName;
	}

	@Override
	public void skipAnalysis() {
		analyzer( AnalyzerConstants.KEYWORD_ANALYZER );
	}

	@Override
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		if ( analyzer == null ) {
			analyzerChecker.failIfNotCompatible();
		}

		QUERY_ACCESSOR.set( innerObject, simpleQueryString );
		DEFAULT_OPERATOR_ACCESSOR.set( innerObject, defaultOperator );

		JsonArray fieldArray = new JsonArray();
		for ( ElasticsearchSimpleQueryStringPredicateBuilderFieldState fieldContext : fields.values() ) {
			fieldArray.add( fieldContext.build() );
		}
		FIELDS_ACCESSOR.set( innerObject, fieldArray );

		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}

		if ( flags != null ) {
			StringBuilder flagsMask = new StringBuilder();
			for ( SimpleQueryFlag flag : flags ) {
				if ( flagsMask.length() > 0 ) {
					flagsMask.append( "|" );
				}
				flagsMask.append( getFlagName( flag ) );
			}
			FLAGS_ACCESSOR.set( innerObject, flagsMask.toString() );
		}

		SIMPLE_QUERY_STRING_ACCESSOR.set( outerObject, innerObject );

		return ( nestedCompatibilityChecker.getNestedPathHierarchy().isEmpty() ) ? outerObject :
				AbstractElasticsearchSearchNestedPredicateBuilder.applyImplicitNested( outerObject, nestedCompatibilityChecker.getNestedPathHierarchy(), context );
	}

	/**
	 * @param flag The flag as defined in Hibernate Search.
	 * @return The name of this flag in Elasticsearch (might be different from flag.name()).
	 */
	private String getFlagName(SimpleQueryFlag flag) {
		switch ( flag ) {
			case AND:
				return "AND";
			case NOT:
				return "NOT";
			case OR:
				return "OR";
			case PREFIX:
				return "PREFIX";
			case PHRASE:
				return "PHRASE";
			case PRECEDENCE:
				return "PRECEDENCE";
			case ESCAPE:
				return "ESCAPE";
			case WHITESPACE:
				return "WHITESPACE";
			case FUZZY:
				return "FUZZY";
			case NEAR:
				return "SLOP";
			default:
				throw new AssertionFailure( "Unexpected flag: " + flag );
		}
	}

}
