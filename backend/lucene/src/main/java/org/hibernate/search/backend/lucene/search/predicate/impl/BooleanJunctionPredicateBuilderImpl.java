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

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Guillaume Smet
 */
class BooleanJunctionPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements BooleanJunctionPredicateBuilder<LuceneSearchPredicateContext, LuceneSearchPredicateCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private List<SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector>> mustContributors;
	private List<SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector>> mustNotContributors;
	private List<SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector>> shouldContributors;
	private List<SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector>> filterContributors;

	private NavigableMap<Integer, MinimumShouldMatchConstraint> minimumShouldMatchConstraints;

	@Override
	public void must(SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> contributor) {
		if ( mustContributors == null ) {
			mustContributors = new ArrayList<>();
		}
		mustContributors.add( contributor );
	}

	@Override
	public void mustNot(SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> contributor) {
		if ( mustNotContributors == null ) {
			mustNotContributors = new ArrayList<>();
		}
		mustNotContributors.add( contributor );
	}

	@Override
	public void should(SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> contributor) {
		if ( shouldContributors == null ) {
			shouldContributors = new ArrayList<>();
		}
		shouldContributors.add( contributor );
	}

	@Override
	public void filter(SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> contributor) {
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
	protected Query buildQuery(LuceneSearchPredicateContext context) {
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		contributeQueries( context, booleanQueryBuilder, mustContributors, Occur.MUST );
		contributeQueries( context, booleanQueryBuilder, mustNotContributors, Occur.MUST_NOT );
		contributeQueries( context, booleanQueryBuilder, shouldContributors, Occur.SHOULD );
		contributeQueries( context, booleanQueryBuilder, filterContributors, Occur.FILTER );

		if ( isOnlyMustNot() ) {
			booleanQueryBuilder.add( new MatchAllDocsQuery(), Occur.FILTER );
		}

		if ( minimumShouldMatchConstraints != null && shouldContributors != null ) {
			int minimumShouldMatch;
			Map.Entry<Integer, MinimumShouldMatchConstraint> entry =
					minimumShouldMatchConstraints.lowerEntry( shouldContributors.size() );
			if ( entry != null ) {
				minimumShouldMatch = entry.getValue().toMinimum( shouldContributors.size() );
			}
			else {
				minimumShouldMatch = shouldContributors.size();
			}
			booleanQueryBuilder.setMinimumNumberShouldMatch( minimumShouldMatch );
		}

		return booleanQueryBuilder.build();
	}

	private void contributeQueries(LuceneSearchPredicateContext context,
			BooleanQuery.Builder booleanQueryBuilder,
			List<SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector>> contributors,
			Occur occur) {
		if ( contributors == null ) {
			return;
		}

		for ( SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> contributor : contributors ) {
			booleanQueryBuilder.add( getQueryFromContributor( context, contributor ), occur );
		}
	}

	private boolean isOnlyMustNot() {
		if ( mustNotContributors == null || mustNotContributors.isEmpty() ) {
			return false;
		}
		if ( mustContributors != null && !mustContributors.isEmpty() ) {
			return false;
		}
		if ( shouldContributors != null && !shouldContributors.isEmpty() ) {
			return false;
		}
		if ( filterContributors != null && !filterContributors.isEmpty() ) {
			return false;
		}
		return true;
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
				throw log.minimumShouldMatchMinimumOutOfBounds( minimum, totalShouldClauseNumber );
			}

			return minimum;
		}
	}
}
