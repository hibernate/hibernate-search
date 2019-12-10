/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public final class IndexNameQuery extends Query {
	private final IndexReaderMetadataResolver metadataResolver;
	private final String indexName;

	public IndexNameQuery(IndexReaderMetadataResolver metadataResolver, String indexName) {
		this.metadataResolver = metadataResolver;
		this.indexName = indexName;
	}

	@Override
	public String toString(String field) {
		return getClass().getName() + "{" + indexName + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		IndexNameQuery other = (IndexNameQuery) obj;
		return indexName.equals( other.indexName );
	}

	@Override
	public int hashCode() {
		return indexName.hashCode();
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) {
		return new ConstantScoreWeight( this, 1.0f ) {
			@Override
			public Scorer scorer(LeafReaderContext context) {
				String leafIndexName = metadataResolver.resolveIndexName( context );
				DocIdSetIterator matchingDocs;
				if ( indexName.equals( leafIndexName ) ) {
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
}
