/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import static org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchCommonMinimumShouldMatchConstraint.formatMinimumShouldMatchConstraints;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerConstants;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

abstract class ElasticsearchCommonQueryStringPredicate extends AbstractElasticsearchNestablePredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asString();
	private static final JsonAccessor<JsonElement> DEFAULT_OPERATOR_ACCESSOR =
			JsonAccessor.root().property( "default_operator" );
	private static final JsonAccessor<JsonArray> FIELDS_ACCESSOR = JsonAccessor.root().property( "fields" ).asArray();
	private static final JsonAccessor<String> ANALYZER_ACCESSOR = JsonAccessor.root().property( "analyzer" ).asString();
	private static final JsonAccessor<String> MINIMUM_SHOULD_MATCH_ACCESSOR =
			JsonAccessor.root().property( "minimum_should_match" ).asString();

	private static final JsonPrimitive AND_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "and" );
	private static final JsonPrimitive OR_OPERATOR_KEYWORD_JSON = new JsonPrimitive( "or" );

	private final List<String> nestedPathHierarchy;
	private final List<String> fieldPaths;

	private final List<JsonPrimitive> fieldNameAndBoosts;
	private final JsonPrimitive defaultOperator;
	private final String queryString;
	private final String analyzer;
	private final Map<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;

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
	protected final JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		QUERY_ACCESSOR.set( innerObject, queryString );
		DEFAULT_OPERATOR_ACCESSOR.set( innerObject, defaultOperator );

		JsonArray fieldArray = new JsonArray();
		for ( JsonPrimitive fieldNameAndBoost : fieldNameAndBoosts ) {
			fieldArray.add( fieldNameAndBoost );
		}
		FIELDS_ACCESSOR.set( innerObject, fieldArray );

		if ( analyzer != null ) {
			ANALYZER_ACCESSOR.set( innerObject, analyzer );
		}


		if ( minimumShouldMatchConstraints != null ) {
			MINIMUM_SHOULD_MATCH_ACCESSOR.set(
					innerObject,
					formatMinimumShouldMatchConstraints( minimumShouldMatchConstraints )
			);
		}

		addSpecificProperties( context, outerObject, innerObject );

		queryNameAccessor().set( outerObject, innerObject );

		return outerObject;
	}

	protected abstract void addSpecificProperties(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject);

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
		protected JsonPrimitive defaultOperator = OR_OPERATOR_KEYWORD_JSON;
		protected String queryString;
		protected String analyzer;
		private Map<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
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
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new ElasticsearchCommonMinimumShouldMatchConstraint( matchingClausesNumber, null )
			);
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new ElasticsearchCommonMinimumShouldMatchConstraint( null, matchingClausesPercent )
			);
		}

		private void addMinimumShouldMatchConstraint(int ignoreConstraintCeiling,
				ElasticsearchCommonMinimumShouldMatchConstraint constraint) {
			if ( minimumShouldMatchConstraints == null ) {
				// We'll need to go through the data in ascending order, so use a TreeMap
				minimumShouldMatchConstraints = new TreeMap<>();
			}
			Object previous = minimumShouldMatchConstraints.put( ignoreConstraintCeiling, constraint );
			if ( previous != null ) {
				throw log.minimumShouldMatchConflictingConstraints( ignoreConstraintCeiling );
			}
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
