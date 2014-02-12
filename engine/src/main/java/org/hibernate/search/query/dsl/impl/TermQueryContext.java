/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.util.automaton.LevenshteinAutomata;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
* @author Emmanuel Bernard
*/
class TermQueryContext {
	private static final Log log = LoggerFactory.make();

	private final Approximation approximation;

	private int maxEditDistance = FuzzyQuery.defaultMaxEdits;
	private int prefixLength = 0;
	private Float threshold;

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

	public Float getThreshold() {
		return threshold;
	}

	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}

	public static enum Approximation {
		EXACT,
		WILDCARD,
		FUZZY
	}
}
