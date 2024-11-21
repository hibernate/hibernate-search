/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public final class MappedTypeNameQuery extends Query {
	private final IndexReaderMetadataResolver metadataResolver;
	private final String mappedTypeName;

	public MappedTypeNameQuery(IndexReaderMetadataResolver metadataResolver, String mappedTypeName) {
		this.metadataResolver = metadataResolver;
		this.mappedTypeName = mappedTypeName;
	}

	@Override
	public String toString(String field) {
		return getClass().getName() + "{" + mappedTypeName + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		MappedTypeNameQuery other = (MappedTypeNameQuery) obj;
		return mappedTypeName.equals( other.mappedTypeName );
	}

	@Override
	public int hashCode() {
		return mappedTypeName.hashCode();
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) {
		return new ConstantScoreWeight( this, 1.0f ) {
			@Override
			public Scorer scorer(LeafReaderContext context) {
				String leafMappedTypeName = metadataResolver.resolveMappedTypeName( context );
				DocIdSetIterator matchingDocs;
				if ( mappedTypeName.equals( leafMappedTypeName ) ) {
					matchingDocs = DocIdSetIterator.all( context.reader().maxDoc() );
				}
				else {
					matchingDocs = DocIdSetIterator.empty();
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
