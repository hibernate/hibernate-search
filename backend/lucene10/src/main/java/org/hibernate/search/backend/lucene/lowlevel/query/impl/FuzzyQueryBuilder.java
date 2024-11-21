/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import static org.apache.lucene.search.BoostAttribute.DEFAULT_BOOST;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;

public final class FuzzyQueryBuilder extends QueryBuilder {
	private final int maxEditDistance;
	private final int prefixLength;

	public FuzzyQueryBuilder(Analyzer analyzer, int maxEditDistance, int prefixLength) {
		super( analyzer );
		this.maxEditDistance = maxEditDistance;
		this.prefixLength = prefixLength;
	}

	@Override
	protected Query newTermQuery(Term term, float boost) {
		Query q = new FuzzyQuery( term, maxEditDistance, prefixLength );
		if ( boost == DEFAULT_BOOST ) {
			return q;
		}
		return new BoostQuery( q, boost );
	}

}
