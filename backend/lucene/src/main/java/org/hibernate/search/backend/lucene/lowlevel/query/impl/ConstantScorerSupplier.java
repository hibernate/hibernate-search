/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import java.io.IOException;

import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.ScorerSupplier;
import org.apache.lucene.search.Weight;

class ConstantScorerSupplier extends ScorerSupplier {
	private final Weight weight;
	private final float score;
	private final ScoreMode scoreMode;
	private final DocIdSetIterator matchingDocs;

	public ConstantScorerSupplier(Weight weight, float score, ScoreMode scoreMode, DocIdSetIterator matchingDocs) {
		this.weight = weight;
		this.score = score;
		this.scoreMode = scoreMode;
		this.matchingDocs = matchingDocs;
	}

	@Override
	public Scorer get(long leadCost) throws IOException {
		return new ConstantScoreScorer( weight, score, scoreMode, matchingDocs );
	}

	@Override
	public long cost() {
		return matchingDocs.cost();
	}
}
