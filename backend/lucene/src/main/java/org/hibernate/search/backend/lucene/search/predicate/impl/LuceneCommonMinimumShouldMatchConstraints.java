/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.spi.MinimumShouldMatchBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public final class LuceneCommonMinimumShouldMatchConstraints implements MinimumShouldMatchBuilder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private NavigableMap<Integer, MinimumShouldMatchConstraint> minimumShouldMatchConstraints;

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

	public boolean isEmpty() {
		return minimumShouldMatchConstraints == null;
	}

	public int minimumShouldMatch(Collection<?> shouldClauses) {
		return minimumShouldMatch( shouldClauses.size() );
	}

	private int minimumShouldMatch(int shouldClauses) {
		Map.Entry<Integer, MinimumShouldMatchConstraint> entry =
				minimumShouldMatchConstraints.lowerEntry( shouldClauses );
		if ( entry != null ) {
			return entry.getValue().toMinimum( shouldClauses );
		}
		else {
			return shouldClauses;
		}
	}

	public Query apply(Query query) {
		if ( minimumShouldMatchConstraints == null ) {
			return query;
		}
		if ( query instanceof BooleanQuery ) {
			BooleanQuery booleanQuery = (BooleanQuery) query;
			int shouldClauses = (int) booleanQuery.clauses().stream().map( BooleanClause::getOccur )
					.filter( BooleanClause.Occur.SHOULD::equals )
					.count();
			int minimumShouldMatch = minimumShouldMatch( shouldClauses );

			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			for ( BooleanClause clause : booleanQuery.clauses() ) {
				builder.add( clause );
			}

			query = builder.setMinimumNumberShouldMatch( minimumShouldMatch ).build();
		}
		return query;
	}

	private static final class MinimumShouldMatchConstraint {
		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
