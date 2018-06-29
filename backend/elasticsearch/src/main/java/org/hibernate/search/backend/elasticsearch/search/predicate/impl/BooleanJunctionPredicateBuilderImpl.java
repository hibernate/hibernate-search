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
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class BooleanJunctionPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements BooleanJunctionPredicateBuilder<Void, ElasticsearchSearchPredicateCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private List<SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector>> mustContributors;
	private List<SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector>> mustNotContributors;
	private List<SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector>> shouldContributors;
	private List<SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector>> filterContributors;

	private static final JsonAccessor<JsonObject> MUST = JsonAccessor.root().property( "must" ).asObject();
	private static final JsonAccessor<JsonObject> MUST_NOT = JsonAccessor.root().property( "must_not" ).asObject();
	private static final JsonAccessor<JsonObject> SHOULD = JsonAccessor.root().property( "should" ).asObject();
	private static final JsonAccessor<JsonObject> FILTER = JsonAccessor.root().property( "filter" ).asObject();

	private static final JsonAccessor<String> MINIMUM_SHOULD_MATCH =
			JsonAccessor.root().property( "minimum_should_match" ).asString();

	private Map<Integer, MinimumShouldMatchConstraint> minimumShouldMatchConstraints;

	@Override
	public void must(SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> contributor) {
		if ( mustContributors == null ) {
			mustContributors = new ArrayList<>();
		}
		mustContributors.add( contributor );
	}

	@Override
	public void mustNot(SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> contributor) {
		if ( mustNotContributors == null ) {
			mustNotContributors = new ArrayList<>();
		}
		mustNotContributors.add( contributor );
	}

	@Override
	public void should(SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> contributor) {
		if ( shouldContributors == null ) {
			shouldContributors = new ArrayList<>();
		}
		shouldContributors.add( contributor );
	}

	@Override
	public void filter(SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> contributor) {
		if ( filterContributors == null ) {
			filterContributors = new ArrayList<>();
		}
		filterContributors.add( contributor );
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
	public void contribute(Void context, ElasticsearchSearchPredicateCollector collector) {
		JsonObject innerObject = getInnerObject();

		contributeQueries( innerObject, MUST, mustContributors );
		contributeQueries( innerObject, MUST_NOT, mustNotContributors );
		contributeQueries( innerObject, SHOULD, shouldContributors );
		contributeQueries( innerObject, FILTER, filterContributors );

		if ( minimumShouldMatchConstraints != null ) {
			MINIMUM_SHOULD_MATCH.set(
					innerObject,
					formatMinimumShouldMatchConstraints( minimumShouldMatchConstraints )
			);
		}

		JsonObject outerObject = getOuterObject();
		outerObject.add( "bool", innerObject );
		collector.collectPredicate( outerObject );
	}

	private void contributeQueries(JsonObject innerObject,
			JsonAccessor<JsonObject> occurAccessor,
			List<SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector>> contributors) {
		if ( contributors == null ) {
			return;
		}

		for ( SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> contributor : contributors ) {
			occurAccessor.add( innerObject, getQueryFromContributor( contributor ) );
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
