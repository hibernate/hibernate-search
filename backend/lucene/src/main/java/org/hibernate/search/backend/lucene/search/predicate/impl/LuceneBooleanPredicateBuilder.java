/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;



class LuceneBooleanPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements BooleanPredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private List<LuceneSearchPredicateBuilder> mustClauseBuilders;
	private List<LuceneSearchPredicateBuilder> mustNotClauseBuilders;
	private List<LuceneSearchPredicateBuilder> shouldClauseBuilders;
	private List<LuceneSearchPredicateBuilder> filterClauseBuilders;

	private NavigableMap<Integer, MinimumShouldMatchConstraint> minimumShouldMatchConstraints;

	@Override
	public void must(LuceneSearchPredicateBuilder clauseBuilder) {
		if ( mustClauseBuilders == null ) {
			mustClauseBuilders = new ArrayList<>();
		}
		mustClauseBuilders.add( clauseBuilder );
	}

	@Override
	public void mustNot(LuceneSearchPredicateBuilder clauseBuilder) {
		if ( mustNotClauseBuilders == null ) {
			mustNotClauseBuilders = new ArrayList<>();
		}
		mustNotClauseBuilders.add( clauseBuilder );
	}

	@Override
	public void should(LuceneSearchPredicateBuilder clauseBuilder) {
		if ( shouldClauseBuilders == null ) {
			shouldClauseBuilders = new ArrayList<>();
		}
		shouldClauseBuilders.add( clauseBuilder );
	}

	@Override
	public void filter(LuceneSearchPredicateBuilder clauseBuilder) {
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
	public void checkNestableWithin(String expectedParentNestedPath) {
		checkNestableWithin( expectedParentNestedPath, mustClauseBuilders );
		checkNestableWithin( expectedParentNestedPath, shouldClauseBuilders );
		checkNestableWithin( expectedParentNestedPath, filterClauseBuilders );
		checkNestableWithin( expectedParentNestedPath, mustNotClauseBuilders );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		contributeQueries( context, booleanQueryBuilder, mustClauseBuilders, Occur.MUST );
		contributeQueries( context, booleanQueryBuilder, mustNotClauseBuilders, Occur.MUST_NOT );
		contributeQueries( context, booleanQueryBuilder, shouldClauseBuilders, Occur.SHOULD );
		contributeQueries( context, booleanQueryBuilder, filterClauseBuilders, Occur.FILTER );

		if ( isOnlyMustNot() ) {
			booleanQueryBuilder.add( new MatchAllDocsQuery(), Occur.FILTER );
		}

		if ( minimumShouldMatchConstraints != null && shouldClauseBuilders != null ) {
			int minimumShouldMatch;
			Map.Entry<Integer, MinimumShouldMatchConstraint> entry =
					minimumShouldMatchConstraints.lowerEntry( shouldClauseBuilders.size() );
			if ( entry != null ) {
				minimumShouldMatch = entry.getValue().toMinimum( shouldClauseBuilders.size() );
			}
			else {
				minimumShouldMatch = shouldClauseBuilders.size();
			}
			booleanQueryBuilder.setMinimumNumberShouldMatch( minimumShouldMatch );
		}

		return booleanQueryBuilder.build();
	}

	private void contributeQueries(LuceneSearchPredicateContext context,
			BooleanQuery.Builder booleanQueryBuilder,
			List<LuceneSearchPredicateBuilder> clauseBuilders,
			Occur occur) {
		if ( clauseBuilders == null ) {
			return;
		}

		for ( LuceneSearchPredicateBuilder clauseBuilder : clauseBuilders ) {
			booleanQueryBuilder.add( clauseBuilder.build( context ), occur );
		}
	}

	private void checkNestableWithin(String expectedParentNestedPath, List<LuceneSearchPredicateBuilder> clauseBuilders) {
		if ( clauseBuilders == null ) {
			return;
		}
		for ( LuceneSearchPredicateBuilder builder : clauseBuilders ) {
			builder.checkNestableWithin( expectedParentNestedPath );
		}
	}

	private boolean isOnlyMustNot() {
		return mustNotClauseBuilders != null && !mustNotClauseBuilders.isEmpty()
				&& ( mustClauseBuilders == null || mustClauseBuilders.isEmpty() )
				&& ( shouldClauseBuilders == null || shouldClauseBuilders.isEmpty() )
				&& ( filterClauseBuilders == null || filterClauseBuilders.isEmpty() );
	}

	private static final class MinimumShouldMatchConstraint {
		private final Integer matchingClausesNumber;
		private final Integer matchingClausesPercent;

		MinimumShouldMatchConstraint(Integer matchingClausesNumber, Integer matchingClausesPercent) {
			this.matchingClausesNumber = matchingClausesNumber;
			this.matchingClausesPercent = matchingClausesPercent;
		}

		int toMinimum(int totalShouldClauseNumber) {
			int minimum;
			if ( matchingClausesNumber != null ) {
				if ( matchingClausesNumber >= 0 ) {
					minimum = matchingClausesNumber;
				}
				else {
					minimum = totalShouldClauseNumber + matchingClausesNumber;
				}
			}
			else {
				if ( matchingClausesPercent >= 0 ) {
					minimum = matchingClausesPercent * totalShouldClauseNumber / 100;
				}
				else {
					minimum = totalShouldClauseNumber + matchingClausesPercent * totalShouldClauseNumber / 100;
				}
			}

			if ( minimum < 1 || minimum > totalShouldClauseNumber ) {
				throw log.minimumShouldMatchMinimumOutOfBounds( totalShouldClauseNumber, minimum );
			}

			return minimum;
		}
	}
}
