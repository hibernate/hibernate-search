/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Guillaume Smet
 */
class BooleanJunctionPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements BooleanJunctionPredicateBuilder<LuceneSearchPredicateCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

	private boolean hasMustNot = false;
	private boolean hasOnlyMustNot = true;
	private int shouldClauseCount = 0;

	private NavigableMap<Integer, MinimumShouldMatchConstraint> minimumShouldMatchConstraints;

	@Override
	public LuceneSearchPredicateCollector getMustCollector() {
		return this::must;
	}

	@Override
	public LuceneSearchPredicateCollector getMustNotCollector() {
		return this::mustNot;
	}

	@Override
	public LuceneSearchPredicateCollector getShouldCollector() {
		return this::should;
	}

	@Override
	public LuceneSearchPredicateCollector getFilterCollector() {
		return this::filter;
	}

	@Override
	public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
		addMinimumShouldMatchConstraint(
				ignoreConstraintCeiling,
				new MinimumShouldMatchConstraint( matchingClausesNumber, null )
		);
	}

	@Override
	public void minimumShouldMatchRatio(int ignoreConstraintCeiling, double matchingClausesRatio) {
		addMinimumShouldMatchConstraint(
				ignoreConstraintCeiling,
				new MinimumShouldMatchConstraint( null, matchingClausesRatio )
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

	private void must(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.MUST );
		hasOnlyMustNot = false;
	}

	private void mustNot(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.MUST_NOT );
		hasMustNot = true;
	}

	private void should(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.SHOULD );
		++shouldClauseCount;
		hasOnlyMustNot = false;
	}

	private void filter(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.FILTER );
		hasOnlyMustNot = false;
	}

	@Override
	protected Query buildQuery() {
		if ( hasMustNot && hasOnlyMustNot ) {
			booleanQueryBuilder.add( new MatchAllDocsQuery(), Occur.FILTER );
		}
		if ( minimumShouldMatchConstraints != null ) {
			int minimumShouldMatch;
			Map.Entry<Integer, MinimumShouldMatchConstraint> entry =
					minimumShouldMatchConstraints.lowerEntry( shouldClauseCount );
			if ( entry != null ) {
				minimumShouldMatch = entry.getValue().toMinimum( shouldClauseCount );
			}
			else {
				minimumShouldMatch = shouldClauseCount;
			}
			booleanQueryBuilder.setMinimumNumberShouldMatch( minimumShouldMatch );
		}
		return booleanQueryBuilder.build();
	}

	private static final class MinimumShouldMatchConstraint {
		private final Integer matchingClausesNumber;
		private final Double matchingClausesRatio;

		MinimumShouldMatchConstraint(Integer matchingClausesNumber, Double matchingClausesRatio) {
			this.matchingClausesNumber = matchingClausesNumber;
			this.matchingClausesRatio = matchingClausesRatio;
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
				if ( matchingClausesRatio >= 0.0 ) {
					minimum = (int) ( matchingClausesRatio * totalShouldClauseNumber );
				}
				else {
					minimum = totalShouldClauseNumber + (int) ( matchingClausesRatio * totalShouldClauseNumber );
				}
			}

			if ( minimum < 1 || minimum > totalShouldClauseNumber ) {
				throw log.minimumShouldMatchMinimumOutOfBounds( minimum, totalShouldClauseNumber );
			}

			return minimum;
		}
	}
}
