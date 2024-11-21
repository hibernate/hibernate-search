/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.query;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FilteredDocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.ScorerSupplier;
import org.apache.lucene.search.Weight;

/**
 * A query that returns all documents, but executes slowly.
 */
public class SlowQuery extends Query {
	private final int millisecondsPerDocument;

	/**
	 * @param millisecondsPerDocument How many milliseconds to wait for each document when iterating over documents.
	 */
	public SlowQuery(int millisecondsPerDocument) {
		this.millisecondsPerDocument = millisecondsPerDocument;
	}

	@Override
	public String toString(String field) {
		return "SlowQuery";
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SlowQuery && ( (SlowQuery) obj ).millisecondsPerDocument == millisecondsPerDocument;
	}

	@Override
	public int hashCode() {
		return millisecondsPerDocument;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) {
		return new ConstantScoreWeight( this, boost ) {
			@Override
			public String toString() {
				return "weight(" + SlowQuery.this + ")";
			}

			@Override
			public ScorerSupplier scorerSupplier(LeafReaderContext context) {
				float score = score();
				SlowDocIdSetIterator iterator = new SlowDocIdSetIterator( DocIdSetIterator.all( context.reader().maxDoc() ) );
				return new ScorerSupplier() {
					@Override
					public Scorer get(long leadCost) throws IOException {
						return new ConstantScoreScorer( score, scoreMode, iterator );
					}

					@Override
					public long cost() {
						return iterator.cost();
					}
				};
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				return false;
			}
		};
	}

	@Override
	public void visit(QueryVisitor visitor) {
		visitor.visitLeaf( this );
	}

	private class SlowDocIdSetIterator extends FilteredDocIdSetIterator {
		public SlowDocIdSetIterator(DocIdSetIterator delegate) {
			super( delegate );
		}

		@Override
		protected boolean match(int doc) {
			try {
				Thread.sleep( millisecondsPerDocument );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException( e );
			}
			return true;
		}

	}
}
