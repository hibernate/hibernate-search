/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;
import org.hibernate.search.spatial.SpatialFieldBridgeByHash;

/**
 * Lucene distance Query for documents which have been indexed with Hibernate Search {@link SpatialFieldBridgeByHash}
 * Use denormalized spatial hash cell ids to return a sub set of documents near the center
 *
 * @author Nicolas Helleringer
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByHash
 * @see org.hibernate.search.spatial.Coordinates
 */
public final class SpatialHashQuery extends Query {

	private final List<String> spatialHashCellsIds;
	private final String fieldName;

	public SpatialHashQuery(List<String> spatialHashCellsIds, String fieldName) {
		this.spatialHashCellsIds = spatialHashCellsIds;
		this.fieldName = fieldName;
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
		return new ConstantScoreWeight( this ) {
			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				DocIdSetIterator iterator = createDocIdSetIterator( context );
				return new ConstantScoreScorer( this, score(), iterator );
			}
		};
	}

	/**
	 * Search the index for document having the correct spatial hash cell id at given grid level.
	 *
	 * @param context the {@link LeafReaderContext} for which to return the {@link DocIdSet}.
	 * @return a {@link DocIdSetIterator} with the matching document ids
	 */
	private DocIdSetIterator createDocIdSetIterator(LeafReaderContext context) throws IOException {
		if ( spatialHashCellsIds.size() == 0 ) {
			return null;
		}

		final LeafReader atomicReader = context.reader();

		BitDocIdSet matchedDocumentsIds = new BitDocIdSet( new FixedBitSet( atomicReader.maxDoc() ) );
		boolean found = false;
		for ( int i = 0; i < spatialHashCellsIds.size(); i++ ) {
			Term spatialHashCellTerm = new Term( fieldName, spatialHashCellsIds.get( i ) );
			PostingsEnum spatialHashCellsDocs = atomicReader.postings( spatialHashCellTerm );
			if ( spatialHashCellsDocs != null ) {
				while ( true ) {
					final int docId = spatialHashCellsDocs.nextDoc();
					if ( docId == DocIdSetIterator.NO_MORE_DOCS ) {
						break;
					}
					else {
						matchedDocumentsIds.bits().set( docId );
						found = true;
					}
				}
			}
		}

		if ( found ) {
			return matchedDocumentsIds.iterator();
		}
		else {
			return DocIdSetIterator.empty();
		}
	}

	public List<String> getSpatialHashCellsIds() {
		return Collections.unmodifiableList( spatialHashCellsIds );
	}

	public String getFieldName() {
		return fieldName;
	}

	@Override
	public int hashCode() {
		int hashCode = 31 * super.hashCode() + spatialHashCellsIds.hashCode();
		hashCode = 31 * hashCode + fieldName.hashCode();
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj instanceof SpatialHashQuery ) {
			SpatialHashQuery other = (SpatialHashQuery) obj;
			return spatialHashCellsIds.equals( other.spatialHashCellsIds )
				&& fieldName.equals( other.fieldName );
		}
		return false;
	}

	@Override
	public String toString(String field) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "SpatialHashFilter" );
		sb.append( "{spatialHashCellsIds=" ).append( spatialHashCellsIds );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
