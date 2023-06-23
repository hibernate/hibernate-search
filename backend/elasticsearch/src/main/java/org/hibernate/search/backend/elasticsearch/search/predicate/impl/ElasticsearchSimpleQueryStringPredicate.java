/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerConstants;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchSimpleQueryStringPredicate extends AbstractElasticsearchNestablePredicate {

	private static final JsonObjectAccessor SIMPLE_QUERY_STRING_ACCESSOR =
			JsonAccessor.root().property( "simple_query_string" ).asObject();
	private static final JsonAccessor<String> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asString();
	private static final JsonAccessor<JsonElement> DEFAULT_OPERATOR_ACCESSOR =
			JsonAccessor.root().property( "default_operator" );
	private static final JsonAccessor<JsonArray> FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asArray();
	private static final JsonAccessor<String> ANALYZER_ACCESSOR = JsonAccessor.root().property( "analyzer" ).asString();
	private static final JsonAccessor<String> FLAGS_ACCESSOR = JsonAccessor.root().property( "flags" ).asString();

	private static final JsonPrimitive AND_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "and" );
	private static final JsonPrimitive OR_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "or" );

	private final List<String> nestedPathHierarchy;
	private final List<String> fieldPaths;

	private final List<JsonPrimitive> fieldNameAndBoosts;
	private final JsonPrimitive defaultOperator;
	private final String simpleQueryString;
	private final String analyzer;
	private final Set<SimpleQueryFlag> flags;

	ElasticsearchSimpleQueryStringPredicate(Builder builder) {
		super( builder );
		nestedPathHierarchy = builder.firstFieldState.field().nestedPathHierarchy();
		// Warning: we must use field().absolutePath(), not the keys in the map,
		// because that key may be a relative path when using SearchPredicateFactory.withRoot(...)
		fieldPaths = new ArrayList<>( builder.fieldStates.size() );
		for ( ElasticsearchSimpleQueryStringPredicateBuilderFieldState state : builder.fieldStates.values() ) {
			fieldPaths.add( state.field().absolutePath() );
		}
		fieldNameAndBoosts = new ArrayList<>();
		for ( ElasticsearchSimpleQueryStringPredicateBuilderFieldState fieldContext : builder.fieldStates.values() ) {
			fieldNameAndBoosts.add( fieldContext.build() );
		}
		defaultOperator = builder.defaultOperator;
		simpleQueryString = builder.simpleQueryString;
		analyzer = builder.analyzer;
		flags = builder.flags;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		QUERY_ACCESSOR.set( innerObject, simpleQueryString );
		DEFAULT_OPERATOR_ACCESSOR.set( innerObject, defaultOperator );

		JsonArray fieldArray = new JsonArray();
		for ( JsonPrimitive fieldNameAndBoost : fieldNameAndBoosts ) {
			fieldArray.add( fieldNameAndBoost );
		}
		FIELDS_ACCESSOR.set( innerObject, fieldArray );

		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}

		if ( flags != null ) {
			FLAGS_ACCESSOR.set( innerObject, toFlagsMask( flags ) );
		}

		SIMPLE_QUERY_STRING_ACCESSOR.set( outerObject, innerObject );

		return outerObject;
	}

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return fieldPaths;
	}

	private static String toFlagsMask(Set<SimpleQueryFlag> flags) {
		if ( flags.isEmpty() ) {
			return "NONE";
		}
		StringBuilder flagsMask = new StringBuilder();
		for ( SimpleQueryFlag flag : flags ) {
			if ( flagsMask.length() > 0 ) {
				flagsMask.append( "|" );
			}
			flagsMask.append( getFlagName( flag ) );
		}
		return flagsMask.toString();
	}

	/**
	 * @param flag The flag as defined in Hibernate Search.
	 * @return The name of this flag in Elasticsearch (might be different from flag.name()).
	 */
	private static String getFlagName(SimpleQueryFlag flag) {
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

	public static class Builder extends AbstractBuilder implements SimpleQueryStringPredicateBuilder {

		private ElasticsearchSimpleQueryStringPredicateBuilderFieldState firstFieldState;
		private final Map<String, ElasticsearchSimpleQueryStringPredicateBuilderFieldState> fieldStates = new LinkedHashMap<>();
		private JsonPrimitive defaultOperator = OR_OPERATOR_KEYWORD_JSON;
		private String simpleQueryString;
		private String analyzer;
		private Set<SimpleQueryFlag> flags;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
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
			this.flags = flags.isEmpty() ? Collections.emptySet() : EnumSet.copyOf( flags );
		}

		@Override
		public FieldState field(String fieldPath) {
			ElasticsearchSimpleQueryStringPredicateBuilderFieldState fieldState = fieldStates.get( fieldPath );
			if ( fieldState == null ) {
				fieldState = scope.fieldQueryElement( fieldPath, ElasticsearchPredicateTypeKeys.SIMPLE_QUERY_STRING );
				if ( firstFieldState == null ) {
					firstFieldState = fieldState;
				}
				else {
					SearchIndexSchemaElementContextHelper.checkNestedDocumentPathCompatibility(
							firstFieldState.field(), fieldState.field() );
				}
				fieldStates.put( fieldPath, fieldState );
			}
			return fieldState;
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
		public SearchPredicate build() {
			if ( analyzer == null ) {
				for ( ElasticsearchSimpleQueryStringPredicateBuilderFieldState field : fieldStates.values() ) {
					field.checkAnalyzerOrNormalizerCompatibleAcrossIndexes();
				}
			}

			return new ElasticsearchSimpleQueryStringPredicate( this );
		}

	}
}
