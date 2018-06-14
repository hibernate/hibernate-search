/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.search.BooleanQuery;

final class MinimumShouldMatchContextImpl {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private MinimumShouldMatchConstraint minimumShouldMatchConstraint;

	public void requireNumber(int matchingClausesNumber) {
		addMinimumShouldMatchConstraint(
				new MinimumShouldMatchConstraint( matchingClausesNumber, null )
		);
	}

	public void requirePercent(int matchingClausesPercent) {
		addMinimumShouldMatchConstraint(
				new MinimumShouldMatchConstraint( null, matchingClausesPercent )
		);
	}

	void applyMinimum(BooleanQuery.Builder builder, int shouldClauseCount) {
		if ( minimumShouldMatchConstraint != null ) {
			int minimumShouldMatch = minimumShouldMatchConstraint.toMinimum( shouldClauseCount );
			builder.setMinimumNumberShouldMatch( minimumShouldMatch );
		}
	}

	private void addMinimumShouldMatchConstraint(MinimumShouldMatchConstraint constraint) {
		if ( this.minimumShouldMatchConstraint != null ) {
			throw log.minimumShouldMatchConflictingConstraints();
		}
		this.minimumShouldMatchConstraint = constraint;
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
