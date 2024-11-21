/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import java.util.Arrays;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public final class ExplicitDocIdsQuery extends Query {
	private final int[] sortedDocIds;

	public ExplicitDocIdsQuery(ScoreDoc[] scoreDocs, int start, int end) {
		int size = end - start;
		int[] docIds = new int[size];
		for ( int i = 0; i < size; i++ ) {
			docIds[i] = scoreDocs[start + i].doc;
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
				if ( matchingDocs == null ) {
					return null; // Skip this leaf
				}
				return new ConstantScoreScorer( this, this.score(), scoreMode, matchingDocs );
			}

			@Override
			public boolean isCacheable(LeafReaderContext ctx) {
				// Not sure what the requirements for caching are: let's not bother.
				return false;
			}
		};
	}

	@Override
	public void visit(QueryVisitor visitor) {
		visitor.visitLeaf( this );
	}

}
