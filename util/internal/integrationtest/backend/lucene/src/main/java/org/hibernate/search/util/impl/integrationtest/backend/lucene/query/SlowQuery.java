/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.query;

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
			public Scorer scorer(LeafReaderContext context) {
				return new ConstantScoreScorer(
						this, score(), scoreMode,
						new SlowDocIdSetIterator( DocIdSetIterator.all( context.reader().maxDoc() ) )
				);
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
