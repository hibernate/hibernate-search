/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import java.util.Arrays;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public final class ExplicitDocIdsQuery extends Query {
	private final int[] sortedDocIds;

	public ExplicitDocIdsQuery(ScoreDoc[] scoreDocs) {
		int[] docIds = new int[scoreDocs.length];
		for ( int i = 0; i < scoreDocs.length; i++ ) {
			docIds[i] = scoreDocs[i].doc;
		}
		Arrays.sort( docIds );
		this.sortedDocIds = docIds;
	}

	@Override
	public String toString(String field) {
		return getClass().getName() + "{" + Arrays.toString( sortedDocIds ) + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		ExplicitDocIdsQuery other = (ExplicitDocIdsQuery) obj;
		return Arrays.equals( sortedDocIds, other.sortedDocIds );
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode( sortedDocIds );
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) {
		return new ConstantScoreWeight( this, 1.0f ) {
			@Override
			public Scorer scorer(LeafReaderContext context) {
				DocIdSetIterator matchingDocs = ExplicitDocIdSetIterator.of(
						sortedDocIds, context.docBase, context.reader().maxDoc()
				);
				return new ConstantScoreScorer( this, this.score(), scoreMode, matchingDocs );
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				// Not sure what the requirements for caching are: let's not bother.
				return false;
			}
		};
	}

}
