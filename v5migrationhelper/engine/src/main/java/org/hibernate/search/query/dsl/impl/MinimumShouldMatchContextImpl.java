/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

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

	void applyMinimum(BooleanPredicateClausesStep<?> step) {
		if ( minimumShouldMatchConstraint != null ) {
			minimumShouldMatchConstraint.apply( step );
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

		void apply(BooleanPredicateClausesStep<?> step) {
			if ( matchingClausesNumber != null ) {
				step.minimumShouldMatchNumber( matchingClausesNumber );
			}
			else {
				step.minimumShouldMatchPercent( matchingClausesPercent );
			}
		}
	}
}
