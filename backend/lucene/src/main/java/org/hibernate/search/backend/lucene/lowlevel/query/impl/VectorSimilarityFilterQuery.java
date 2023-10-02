/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import java.io.IOException;
import java.util.Objects;

import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.FilterWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnByteVectorQuery;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.VectorUtil;

public class VectorSimilarityFilterQuery extends Query {

	private final Query query;
	private final float similarityAsScore;

	public static VectorSimilarityFilterQuery create(KnnByteVectorQuery query, float similarityLimit, int dimension,
			VectorSimilarityFunction vectorSimilarityFunction) {
		// We assume that `similarityLimit` is a distance so we need to convert it to the score using a formula from a
		// similarity function:
		return new VectorSimilarityFilterQuery(
				query, byteSimilarityDistanceToScore( similarityLimit, dimension, vectorSimilarityFunction ) );
	}

	public static VectorSimilarityFilterQuery create(KnnFloatVectorQuery query, float similarityLimit,
			VectorSimilarityFunction vectorSimilarityFunction) {
		// We assume that `similarityLimit` is a distance so we need to convert it to the score using a formula from a
		// similarity function:
		return new VectorSimilarityFilterQuery(
				query, floatSimilarityDistanceToScore( similarityLimit, vectorSimilarityFunction ) );
	}

	private VectorSimilarityFilterQuery(Query query, float similarityAsScore) {
		this.query = query;
		this.similarityAsScore = similarityAsScore;
	}

	@Override
	public Query rewrite(IndexSearcher indexSearcher) throws IOException {
		Query rewritten = query.rewrite( indexSearcher );
		if ( rewritten == query ) {
			return this;
		}
		// Knn queries are rewritten and we need to use a rewritten one to get the weights and scores:
		return new VectorSimilarityFilterQuery( rewritten, this.similarityAsScore );
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
		// we've already converted distance/similarity to a score, but now if the underlying query is boosting the score,
		// we'd want to boost our converted one as well to get the expected matches:
		return new SimilarityWeight( query.createWeight( searcher, scoreMode, boost ), similarityAsScore * boost );
	}

	@Override
	public void visit(QueryVisitor visitor) {
		visitor.visitLeaf( this );
	}

	@Override
	public String toString(String field) {
		return getClass().getName() + "{" +
				"query=" + query +
				", similarityLimit=" + similarityAsScore +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		VectorSimilarityFilterQuery that = (VectorSimilarityFilterQuery) o;
		return Float.compare( similarityAsScore, that.similarityAsScore ) == 0 && Objects.equals( query, that.query );
	}

	@Override
	public int hashCode() {
		return Objects.hash( query, similarityAsScore );
	}

	private static float floatSimilarityDistanceToScore(float distance, VectorSimilarityFunction similarityFunction) {
		switch ( similarityFunction ) {
			case EUCLIDEAN:
				return 1.0f / ( 1.0f + distance * distance );
			case DOT_PRODUCT:
			case COSINE:
				return ( 1.0f + distance ) / 2.0f;
			case MAXIMUM_INNER_PRODUCT:
				return VectorUtil.scaleMaxInnerProductScore( distance );
			default:
				throw new AssertionFailure( "Unknown similarity function: " + similarityFunction );
		}
	}

	private static float byteSimilarityDistanceToScore(float distance, int dimension,
			VectorSimilarityFunction similarityFunction) {
		switch ( similarityFunction ) {
			case EUCLIDEAN:
				return 1.0f / ( 1.0f + distance * distance );
			case DOT_PRODUCT:
				return 0.5f + distance / (float) ( dimension * ( 1 << 15 ) );
			case COSINE:
				return ( 1.0f + distance ) / 2.0f;
			case MAXIMUM_INNER_PRODUCT:
				return VectorUtil.scaleMaxInnerProductScore( distance );
			default:
				throw new AssertionFailure( "Unknown similarity function: " + similarityFunction );
		}
	}

	private static class SimilarityWeight extends FilterWeight {
		private final float similarityAsScore;

		protected SimilarityWeight(Weight weight, float similarityAsScore) {
			super( weight );
			this.similarityAsScore = similarityAsScore;
		}

		@Override
		public Explanation explain(LeafReaderContext context, int doc) throws IOException {
			Explanation explanation = super.explain( context, doc );
			if ( explanation.isMatch() && similarityAsScore > explanation.getValue().floatValue() ) {
				return Explanation.noMatch( "Similarity limit is greater than the vector similarity.", explanation );
			}
			return explanation;
		}

		@Override
		public Scorer scorer(LeafReaderContext context) throws IOException {
			Scorer scorer = super.scorer( context );
			if ( scorer == null ) {
				return null;
			}
			return new MinScoreScorer( this, scorer, similarityAsScore );
		}
	}

	// An adapted version of `org.opensearch.common.lucene.search.function.MinScoreScorer`:
	private static class MinScoreScorer extends Scorer {
		private final Scorer in;
		private final float minScore;
		private float curScore;

		MinScoreScorer(Weight weight, Scorer scorer, float minScore) {
			super( weight );
			this.in = scorer;
			this.minScore = minScore;
		}

		@Override
		public int docID() {
			return in.docID();
		}

		@Override
		public float score() {
			return curScore;
		}

		@Override
		public int advanceShallow(int target) throws IOException {
			return in.advanceShallow( target );
		}

		@Override
		public float getMaxScore(int upTo) throws IOException {
			return in.getMaxScore( upTo );
		}

		@Override
		public DocIdSetIterator iterator() {
			return TwoPhaseIterator.asDocIdSetIterator( twoPhaseIterator() );
		}

		@Override
		public TwoPhaseIterator twoPhaseIterator() {
			final TwoPhaseIterator inTwoPhase = this.in.twoPhaseIterator();
			final DocIdSetIterator approximation = inTwoPhase == null ? in.iterator() : inTwoPhase.approximation();
			return new TwoPhaseIterator( approximation ) {

				@Override
				public boolean matches() throws IOException {
					// we need to check the two-phase iterator first
					// otherwise calling score() is illegal
					if ( inTwoPhase != null && !inTwoPhase.matches() ) {
						return false;
					}
					curScore = in.score();
					return curScore >= minScore;
				}

				@Override
				public float matchCost() {
					return 1000f // random constant for the score computation
							+ ( inTwoPhase == null ? 0 : inTwoPhase.matchCost() );
				}
			};
		}
	}
}
