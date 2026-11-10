package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerConstants;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

abstract class ElasticsearchCommonQueryStringPredicate extends AbstractElasticsearchNestablePredicate {

	private static final JsonAccessor<String> QUERY_ACCESSOR =
			JsonAccessor.root().property( "query" ).unwrapSingletonArrays().asString();
	private static final JsonAccessor<JsonNode> DEFAULT_OPERATOR_ACCESSOR =
			JsonAccessor.root().property( "default_operator" );
	private static final JsonAccessor<ArrayNode> FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asArray();
	private static final JsonAccessor<String> ANALYZER_ACCESSOR =
			JsonAccessor.root().property( "analyzer" ).unwrapSingletonArrays().asString();
	private static final JsonAccessor<String> MINIMUM_SHOULD_MATCH_ACCESSOR =
			JsonAccessor.root().property( "minimum_should_match" ).unwrapSingletonArrays().asString();

	private static final JsonNode AND_OPERATOR_KEYWORD_JSON = JsonScalarConversions.toNode( "and" );
	private static final JsonNode OR_OPERATOR_KEYWORD_JSON = JsonScalarConversions.toNode( "or" );

	private final List<String> nestedPathHierarchy;
	private final List<String> fieldPaths;

	private final List<JsonNode> fieldNameAndBoosts;
	private final JsonNode defaultOperator;
	private final String queryString;
	private final String analyzer;
	private final ElasticsearchCommonMinimumShouldMatchConstraints minimumShouldMatchConstraints;

	ElasticsearchCommonQueryStringPredicate(Builder builder) {
		super( builder );
		nestedPathHierarchy = builder.firstFieldState.field().nestedPathHierarchy();
		// Warning: we must use field().absolutePath(), not the keys in the map,
		// because that key may be a relative path when using SearchPredicateFactory.withRoot(...)
		fieldPaths = new ArrayList<>( builder.fieldStates.size() );
		for ( ElasticsearchCommonQueryStringPredicateBuilderFieldState state : builder.fieldStates.values() ) {
			fieldPaths.add( state.field().absolutePath() );
		}
		fieldNameAndBoosts = new ArrayList<>();
		for ( ElasticsearchCommonQueryStringPredicateBuilderFieldState fieldContext : builder.fieldStates.values() ) {
			fieldNameAndBoosts.add( fieldContext.build() );
		}
		defaultOperator = builder.defaultOperator;
		queryString = builder.queryString;
		analyzer = builder.analyzer;
		minimumShouldMatchConstraints = builder.minimumShouldMatchConstraints;

		builder.minimumShouldMatchConstraints = null;
	}

	@Override
	protected final ObjectNode doToJsonQuery(PredicateRequestContext context, ObjectNode outerObject,
			ObjectNode innerObject) {
		QUERY_ACCESSOR.set( innerObject, queryString );
		DEFAULT_OPERATOR_ACCESSOR.set( innerObject, defaultOperator );

		ArrayNode fieldArray = JsonNodeFactory.instance.arrayNode();
		for ( JsonNode fieldNameAndBoost : fieldNameAndBoosts ) {
			fieldArray.add( fieldNameAndBoost );
		}
		FIELDS_ACCESSOR.set( innerObject, fieldArray );

		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}

		if ( !minimumShouldMatchConstraints.isEmpty() ) {
			MINIMUM_SHOULD_MATCH_ACCESSOR.set(
					innerObject,
					minimumShouldMatchConstraints.formatMinimumShouldMatchConstraints()
			);
		}

		addSpecificProperties( context, outerObject, innerObject );

		queryNameAccessor().set( outerObject, innerObject );

		return outerObject;
	}

	protected abstract void addSpecificProperties(PredicateRequestContext context, ObjectNode outerObject,
			ObjectNode innerObject);

	protected abstract JsonObjectAccessor queryNameAccessor();

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return fieldPaths;
	}


	public abstract static class Builder extends AbstractBuilder implements CommonQueryStringPredicateBuilder {

		protected ElasticsearchCommonQueryStringPredicateBuilderFieldState firstFieldState;
		protected final Map<String, ElasticsearchCommonQueryStringPredicateBuilderFieldState> fieldStates =
				new LinkedHashMap<>();
		protected JsonNode defaultOperator = OR_OPERATOR_KEYWORD_JSON;
		protected String queryString;
		protected String analyzer;
		private ElasticsearchCommonMinimumShouldMatchConstraints minimumShouldMatchConstraints;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
			this.minimumShouldMatchConstraints = new ElasticsearchCommonMinimumShouldMatchConstraints();
		}

		@Override
		public final void defaultOperator(BooleanOperator operator) {
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
		public final void queryString(String queryString) {
			this.queryString = queryString;
		}

		@Override
		public final FieldState field(String fieldPath) {
			ElasticsearchCommonQueryStringPredicateBuilderFieldState fieldState = fieldStates.get( fieldPath );
			if ( fieldState == null ) {
				fieldState = scope.fieldQueryElement( fieldPath, typeKey() );
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
		public final void analyzer(String analyzerName) {
			this.analyzer = analyzerName;
		}

		@Override
		public final void skipAnalysis() {
			analyzer( AnalyzerConstants.KEYWORD_ANALYZER );
		}

		@Override
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			minimumShouldMatchConstraints.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			minimumShouldMatchConstraints.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
		}

		@Override
		public final SearchPredicate build() {
			if ( analyzer == null ) {
				for ( ElasticsearchCommonQueryStringPredicateBuilderFieldState field : fieldStates.values() ) {
					field.checkAnalyzerOrNormalizerCompatibleAcrossIndexes();
				}
			}

			return doBuild( this );
		}

		protected abstract SearchPredicate doBuild(Builder builder);

		protected abstract SearchQueryElementTypeKey<ElasticsearchCommonQueryStringPredicateBuilderFieldState> typeKey();
	}
}
