/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;



class ElasticsearchBooleanJunctionPredicateBuilder extends AbstractElasticsearchSearchPredicateBuilder
		implements BooleanJunctionPredicateBuilder<ElasticsearchSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonObject> MUST_ACCESSOR = JsonAccessor.root().property( "must" ).asObject();
	private static final JsonAccessor<JsonObject> MUST_NOT_ACCESSOR = JsonAccessor.root().property( "must_not" ).asObject();
	private static final JsonAccessor<JsonObject> SHOULD_ACCESSOR = JsonAccessor.root().property( "should" ).asObject();
	private static final JsonAccessor<JsonObject> FILTER_ACCESSOR = JsonAccessor.root().property( "filter" ).asObject();

	private static final JsonAccessor<String> MINIMUM_SHOULD_MATCH_ACCESSOR =
			JsonAccessor.root().property( "minimum_should_match" ).asString();

	private List<ElasticsearchSearchPredicateBuilder> mustClauseBuilders;
	private List<ElasticsearchSearchPredicateBuilder> mustNotClauseBuilders;
	private List<ElasticsearchSearchPredicateBuilder> shouldClauseBuilders;
	private List<ElasticsearchSearchPredicateBuilder> filterClauseBuilders;

	private Map<Integer, MinimumShouldMatchConstraint> minimumShouldMatchConstraints;

	@Override
	public void must(ElasticsearchSearchPredicateBuilder clauseBuilder) {
		if ( mustClauseBuilders == null ) {
			mustClauseBuilders = new ArrayList<>();
		}
		mustClauseBuilders.add( clauseBuilder );
	}

	@Override
	public void mustNot(ElasticsearchSearchPredicateBuilder clauseBuilder) {
		if ( mustNotClauseBuilders == null ) {
			mustNotClauseBuilders = new ArrayList<>();
		}
		mustNotClauseBuilders.add( clauseBuilder );
	}

	@Override
	public void should(ElasticsearchSearchPredicateBuilder clauseBuilder) {
		if ( shouldClauseBuilders == null ) {
			shouldClauseBuilders = new ArrayList<>();
		}
		shouldClauseBuilders.add( clauseBuilder );
	}

	@Override
	public void filter(ElasticsearchSearchPredicateBuilder clauseBuilder) {
		if ( filterClauseBuilders == null ) {
			filterClauseBuilders = new ArrayList<>();
		}
		filterClauseBuilders.add( clauseBuilder );
	}

	@Override
	public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
		addMinimumShouldMatchConstraint(
				ignoreConstraintCeiling,
				new MinimumShouldMatchConstraint( matchingClausesNumber, null )
		);
	}

	@Override
	public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
		addMinimumShouldMatchConstraint(
				ignoreConstraintCeiling,
				new MinimumShouldMatchConstraint( null, matchingClausesPercent )
		);
	}

	private void addMinimumShouldMatchConstraint(int ignoreConstraintCeiling,
			MinimumShouldMatchConstraint constraint) {
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
	protected JsonObject doBuild(ElasticsearchSearchPredicateContext context,
			JsonObject outerObject, JsonObject innerObject) {
		contributeClauses( context, innerObject, MUST_ACCESSOR, mustClauseBuilders );
		contributeClauses( context, innerObject, MUST_NOT_ACCESSOR, mustNotClauseBuilders );
		contributeClauses( context, innerObject, SHOULD_ACCESSOR, shouldClauseBuilders );
		contributeClauses( context, innerObject, FILTER_ACCESSOR, filterClauseBuilders );

		// Forcing to the Lucene's defaults. See HSEARCH-3534
		if ( minimumShouldMatchConstraints == null && hasAtLeastOneMustOrFilterPredicate() ) {
			minimumShouldMatchNumber( 0, 0 );
		}

		if ( minimumShouldMatchConstraints != null ) {
			MINIMUM_SHOULD_MATCH_ACCESSOR.set(
					innerObject,
					formatMinimumShouldMatchConstraints( minimumShouldMatchConstraints )
			);
		}

		outerObject.add( "bool", innerObject );

		return outerObject;
	}

	private boolean hasAtLeastOneMustOrFilterPredicate() {
		return mustClauseBuilders != null || filterClauseBuilders != null;
	}

	private void contributeClauses(ElasticsearchSearchPredicateContext context, JsonObject innerObject,
			JsonAccessor<JsonObject> occurAccessor,
			List<ElasticsearchSearchPredicateBuilder> clauseBuilders) {
		if ( clauseBuilders == null ) {
			return;
		}

		for ( ElasticsearchSearchPredicateBuilder clauseBuilder : clauseBuilders ) {
			occurAccessor.add( innerObject, clauseBuilder.build( context ) );
		}
	}

	private String formatMinimumShouldMatchConstraints(Map<Integer, MinimumShouldMatchConstraint> minimumShouldMatchConstraints) {
		StringBuilder builder = new StringBuilder();
		Iterator<Map.Entry<Integer, MinimumShouldMatchConstraint>> iterator =
				minimumShouldMatchConstraints.entrySet().iterator();

		// Process the first constraint differently
		Map.Entry<Integer, MinimumShouldMatchConstraint> entry = iterator.next();
		Integer ignoreConstraintCeiling = entry.getKey();
		MinimumShouldMatchConstraint constraint = entry.getValue();
		if ( ignoreConstraintCeiling.equals( 0 ) && minimumShouldMatchConstraints.size() == 1 ) {
			// Special case: if there's only one constraint and its ignore ceiling is 0, do not mention the ceiling
			constraint.appendTo( builder, null );
			return builder.toString();
		}
		else {
			entry.getValue().appendTo( builder, ignoreConstraintCeiling );
		}

		// Process the other constraints normally
		while ( iterator.hasNext() ) {
			entry = iterator.next();
			ignoreConstraintCeiling = entry.getKey();
			constraint = entry.getValue();
			builder.append( ' ' );
			constraint.appendTo( builder, ignoreConstraintCeiling );
		}

		return builder.toString();
	}

	private static final class MinimumShouldMatchConstraint {
		private final Integer matchingClausesNumber;
		private final Integer matchingClausesPercent;

		MinimumShouldMatchConstraint(Integer matchingClausesNumber, Integer matchingClausesPercent) {
			this.matchingClausesNumber = matchingClausesNumber;
			this.matchingClausesPercent = matchingClausesPercent;
		}

		/**
		 * Format the constraint according to
		 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-minimum-should-match.html">
		 * the format specified in the Elasticsearch documentation
		 * </a>.
		 *
		 * @param builder The builder to append the formatted value to.
		 * @param ignoreConstraintCeiling The ceiling above which this constraint is no longer ignored.
		 */
		void appendTo(StringBuilder builder, Integer ignoreConstraintCeiling) {
			if ( ignoreConstraintCeiling != null ) {
				builder.append( ignoreConstraintCeiling ).append( '<' );
			}
			if ( matchingClausesNumber != null ) {
				builder.append( matchingClausesNumber );
			}
			else {
				builder.append( matchingClausesPercent ).append( '%' );
			}
		}
	}

}
