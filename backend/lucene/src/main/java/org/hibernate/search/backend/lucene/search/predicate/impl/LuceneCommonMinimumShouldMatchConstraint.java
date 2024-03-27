/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class LuceneCommonMinimumShouldMatchConstraint {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Integer matchingClausesNumber;
	private final Integer matchingClausesPercent;

	LuceneCommonMinimumShouldMatchConstraint(Integer matchingClausesNumber, Integer matchingClausesPercent) {
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

	static int minimumShouldMatch(NavigableMap<Integer, LuceneCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints,
			Collection<?> shouldClauses) {
		return minimumShouldMatch( minimumShouldMatchConstraints, shouldClauses.size() );
	}

	static int minimumShouldMatch(NavigableMap<Integer, LuceneCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints,
			int shouldClauses) {
		Map.Entry<Integer, LuceneCommonMinimumShouldMatchConstraint> entry =
				minimumShouldMatchConstraints.lowerEntry( shouldClauses );
		if ( entry != null ) {
			return entry.getValue().toMinimum( shouldClauses );
		}
		else {
			return shouldClauses;
		}
	}
}
