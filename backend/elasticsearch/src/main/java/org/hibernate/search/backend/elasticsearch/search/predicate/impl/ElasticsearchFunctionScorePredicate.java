package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldValueFactorFunction;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.FieldValueFactorFunctionBuilder;
import org.hibernate.search.engine.search.predicate.spi.FunctionScorePredicateBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ElasticsearchFunctionScorePredicate extends AbstractElasticsearchPredicate {

	private static final JsonObjectAccessor FUNCTION_SCORE_ACCESSOR =
			JsonAccessor.root().property( "function_score" ).asObject();
	private static final JsonObjectAccessor QUERY_ACCESSOR = JsonAccessor.root().property( "query" ).asObject();
	private static final JsonArrayAccessor FUNCTIONS_ACCESSOR = JsonAccessor.root().property( "functions" ).asArray();
	private static final JsonAccessor<String> SCORE_MODE_ACCESSOR = JsonAccessor.root().property( "score_mode" ).asString();
	private static final JsonAccessor<String> BOOST_MODE_ACCESSOR = JsonAccessor.root().property( "boost_mode" ).asString();
	private static final JsonAccessor<Float> WEIGHT_ACCESSOR = JsonAccessor.root().property( "weight" ).asFloat();
	private static final JsonObjectAccessor FILTER_ACCESSOR = JsonAccessor.root().property( "filter" ).asObject();
	private static final JsonObjectAccessor FIELD_VALUE_FACTOR_ACCESSOR =
			JsonAccessor.root().property( "field_value_factor" ).asObject();

	private final ElasticsearchSearchPredicate innerPredicate;
	private final List<FunctionEntry> functions;
	private final ScoreMode scoreMode;
	private final BoostMode boostMode;
	private final List<String> nestedPathHierarchy;
	private final List<String> fieldPaths;

	private ElasticsearchFunctionScorePredicate(Builder builder) {
		super( builder );
		this.innerPredicate = builder.innerPredicate;
		this.functions = List.copyOf( builder.functions );
		this.scoreMode = builder.scoreMode;
		this.boostMode = builder.boostMode;
		this.nestedPathHierarchy = builder.resolvedNestedPathHierarchy;
		List<String> fieldPaths = new ArrayList<>();
		for ( FunctionEntry entry : functions ) {
			if ( entry instanceof FieldValueFactorEntry fieldValueFactorEntry ) {
				fieldPaths.add( fieldValueFactorEntry.fvfBuilder.fieldPath() );
			}
		}
		this.fieldPaths = List.copyOf( fieldPaths );
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		String expectedParentNestedPath = context.getNestedPath();
		// Weight-only functions have no field context and are valid in any nested object.
		if ( !fieldPaths.isEmpty()
				&& expectedParentNestedPath != null
				&& !nestedPathHierarchy.contains( expectedParentNestedPath ) ) {
			throw QueryLog.INSTANCE.invalidNestedObjectPathForPredicate( this, expectedParentNestedPath, fieldPaths );
		}
		if ( innerPredicate != null ) {
			innerPredicate.checkNestableWithin( context );
		}
		for ( FunctionEntry entry : functions ) {
			if ( entry.filter != null ) {
				entry.filter.checkNestableWithin( context );
			}
		}
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		JsonElement boost = innerObject.remove( "boost" );
		PredicateRequestContext contextAfterImplicitNesting = nestedPathHierarchy.isEmpty()
				? context
				: context.withNestedPath( nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 ) );
		if ( innerPredicate != null ) {
			innerPredicate.checkNestableWithin( PredicateNestingContext.nested( contextAfterImplicitNesting.getNestedPath() ) );
			QUERY_ACCESSOR.set( innerObject, innerPredicate.toJsonQuery( contextAfterImplicitNesting ) );
		}

		JsonArray functionsArray = new JsonArray();
		for ( FunctionEntry entry : functions ) {
			functionsArray.add( entry.toJson( contextAfterImplicitNesting ) );
		}
		if ( functions.size() == 1 && ( scoreMode == ScoreMode.AVG || scoreMode == ScoreMode.SUM ) ) {
			// Elasticsearch ignores score_mode for a single unfiltered function.
			// A non-matching second function preserves weighted averages and zero-weight sums,
			// without contributing either a score or a weight to the result.
			JsonObject ignoredFunction = new JsonObject();
			JsonObject filter = new JsonObject();
			filter.add( "match_none", new JsonObject() );
			ignoredFunction.add( "filter", filter );
			ignoredFunction.addProperty( "weight", 1 );
			functionsArray.add( ignoredFunction );
		}
		FUNCTIONS_ACCESSOR.set( innerObject, functionsArray );

		if ( scoreMode != null ) {
			SCORE_MODE_ACCESSOR.set( innerObject, scoreMode.jsonValue );
		}
		if ( boostMode != null ) {
			BOOST_MODE_ACCESSOR.set( innerObject, boostMode.jsonValue );
		}

		FUNCTION_SCORE_ACCESSOR.set( outerObject, innerObject );

		JsonObject result = outerObject;

		if ( !nestedPathHierarchy.isEmpty() ) {
			for ( int i = nestedPathHierarchy.size() - 1; i >= 0; i-- ) {
				String path = nestedPathHierarchy.get( i );
				if ( path.equals( context.getNestedPath() ) ) {
					break;
				}
				JsonObject nestedOuter = new JsonObject();
				JsonObject nestedInner = new JsonObject();
				ElasticsearchNestedPredicate.wrap( indexNames(), path, nestedOuter, nestedInner, result );
				result = nestedOuter;
			}
		}

		if ( boost != null ) {
			// Native function_score.boost affects the inner query score. Apply the predicate
			// boost through a separate function, after score combination and implicit nesting.
			JsonObject boosted = new JsonObject();
			QUERY_ACCESSOR.set( boosted, result );
			WEIGHT_ACCESSOR.set( boosted, boost.getAsFloat() );
			BOOST_MODE_ACCESSOR.set( boosted, BoostMode.MULTIPLY.jsonValue );
			result = new JsonObject();
			FUNCTION_SCORE_ACCESSOR.set( result, boosted );
		}

		return result;
	}

	private enum ScoreMode {
		MULTIPLY( "multiply" ),
		SUM( "sum" ),
		AVG( "avg" ),
		FIRST( "first" ),
		MAX( "max" ),
		MIN( "min" );

		private final String jsonValue;

		ScoreMode(String jsonValue) {
			this.jsonValue = jsonValue;
		}
	}

	private enum BoostMode {
		MULTIPLY( "multiply" ),
		REPLACE( "replace" ),
		SUM( "sum" ),
		AVG( "avg" ),
		MAX( "max" ),
		MIN( "min" );

		private final String jsonValue;

		BoostMode(String jsonValue) {
			this.jsonValue = jsonValue;
		}
	}

	static class Builder extends AbstractBuilder implements FunctionScorePredicateBuilder {

		private final ElasticsearchSearchIndexScope<?> scope;
		private ElasticsearchSearchPredicate innerPredicate;
		private final List<FunctionEntry> functions = new ArrayList<>();
		private ScoreMode scoreMode;
		private BoostMode boostMode;
		private List<String> resolvedNestedPathHierarchy = Collections.emptyList();
		private boolean nestedPathHierarchyResolved;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
			this.scope = scope;
		}

		@Override
		public void innerPredicate(SearchPredicate predicate) {
			this.innerPredicate = ElasticsearchSearchPredicate.from( scope, predicate );
		}

		@Override
		public void addFieldValueFactorFunction(FieldValueFactorFunctionBuilder functionBuilder,
				float weight, SearchPredicate filter) {
			ElasticsearchFieldValueFactorFunction.Builder<?> fvfBuilder =
					(ElasticsearchFieldValueFactorFunction.Builder<?>) functionBuilder;
			validateNestedPathHierarchy( fvfBuilder.nestedPathHierarchy(), fvfBuilder.fieldPath() );
			functions.add( new FieldValueFactorEntry( fvfBuilder, weight, toEsPredicate( filter ) ) );
		}

		@Override
		public void addWeightFunction(float weight, SearchPredicate filter) {
			functions.add( new WeightEntry( weight, toEsPredicate( filter ) ) );
		}

		private void validateNestedPathHierarchy(List<String> fieldNestedPathHierarchy, String fieldPath) {
			if ( !nestedPathHierarchyResolved ) {
				resolvedNestedPathHierarchy = fieldNestedPathHierarchy;
				nestedPathHierarchyResolved = true;
				return;
			}
			if ( !Objects.equals( resolvedNestedPathHierarchy, fieldNestedPathHierarchy ) ) {
				throw QueryLog.INSTANCE.incompatibleFunctionScoreNestedPaths( resolvedNestedPathHierarchy, fieldPath,
						fieldNestedPathHierarchy, scope.eventContext() );
			}
		}

		@Override
		public void scoreModeMultiply() {
			this.scoreMode = ScoreMode.MULTIPLY;
		}

		@Override
		public void scoreModeSum() {
			this.scoreMode = ScoreMode.SUM;
		}

		@Override
		public void scoreModeAvg() {
			this.scoreMode = ScoreMode.AVG;
		}

		@Override
		public void scoreModeFirst() {
			this.scoreMode = ScoreMode.FIRST;
		}

		@Override
		public void scoreModeMax() {
			this.scoreMode = ScoreMode.MAX;
		}

		@Override
		public void scoreModeMin() {
			this.scoreMode = ScoreMode.MIN;
		}

		@Override
		public void boostModeMultiply() {
			this.boostMode = BoostMode.MULTIPLY;
		}

		@Override
		public void boostModeReplace() {
			this.boostMode = BoostMode.REPLACE;
		}

		@Override
		public void boostModeSum() {
			this.boostMode = BoostMode.SUM;
		}

		@Override
		public void boostModeAvg() {
			this.boostMode = BoostMode.AVG;
		}

		@Override
		public void boostModeMax() {
			this.boostMode = BoostMode.MAX;
		}

		@Override
		public void boostModeMin() {
			this.boostMode = BoostMode.MIN;
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchFunctionScorePredicate( this );
		}

		private ElasticsearchSearchPredicate toEsPredicate(SearchPredicate predicate) {
			return predicate == null ? null : ElasticsearchSearchPredicate.from( scope, predicate );
		}
	}

	private abstract static class FunctionEntry {
		final float weight;
		final ElasticsearchSearchPredicate filter;

		FunctionEntry(float weight, ElasticsearchSearchPredicate filter) {
			this.weight = weight;
			this.filter = filter;
		}

		JsonObject toJson(PredicateRequestContext context) {
			JsonObject entry = new JsonObject();
			contributeFunction( entry );
			if ( weight != 1.0f ) {
				WEIGHT_ACCESSOR.set( entry, weight );
			}
			if ( filter != null ) {
				FILTER_ACCESSOR.set( entry, filter.toJsonQuery( context ) );
			}
			return entry;
		}

		abstract void contributeFunction(JsonObject entry);
	}

	private static class FieldValueFactorEntry extends FunctionEntry {
		private final ElasticsearchFieldValueFactorFunction.Builder<?> fvfBuilder;

		FieldValueFactorEntry(ElasticsearchFieldValueFactorFunction.Builder<?> fvfBuilder,
				float weight, ElasticsearchSearchPredicate filter) {
			super( weight, filter );
			this.fvfBuilder = fvfBuilder;
		}

		@Override
		void contributeFunction(JsonObject entry) {
			FIELD_VALUE_FACTOR_ACCESSOR.set( entry, fvfBuilder.toJson() );
		}
	}

	private static class WeightEntry extends FunctionEntry {
		WeightEntry(float weight, ElasticsearchSearchPredicate filter) {
			super( weight, filter );
		}

		@Override
		void contributeFunction(JsonObject entry) {
			WEIGHT_ACCESSOR.set( entry, weight );
		}
	}

}
