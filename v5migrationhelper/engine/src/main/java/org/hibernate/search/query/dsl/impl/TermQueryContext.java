/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.util.automaton.LevenshteinAutomata;

/**
* @author Emmanuel Bernard
*/
class TermQueryContext {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final Approximation approximation;

	private int maxEditDistance = FuzzyQuery.defaultMaxEdits;
	private int prefixLength = 0;

	public TermQueryContext(Approximation approximation) {
		this.approximation = approximation;
	}

	public void setPrefixLength(int prefixLength) {
		this.prefixLength = prefixLength;
	}

	public Approximation getApproximation() {
		return approximation;
	}

	public int getMaxEditDistance() {
		return maxEditDistance;
	}

	public void setMaxEditDistance(int maxEditDistance) {
		if ( maxEditDistance < 1 || maxEditDistance > LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE ) {
			throw log.incorrectEditDistance();
		}
		this.maxEditDistance = maxEditDistance;
	}

	public int getPrefixLength() {
		return prefixLength;
	}

	public enum Approximation {
		EXACT,
		WILDCARD,
		FUZZY
	}
}
