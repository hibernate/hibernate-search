/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;

/**
 * Lucene Filter for filtering documents which have been indexed with Hibernate Search Spatial SpatialFieldBridge
 * Use denormalized spatial hash cell ids to return a sub set of documents near the center
 *
 * @author Nicolas Helleringer
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByHash
 * @see org.hibernate.search.spatial.Coordinates
 */
public final class SpatialHashFilter extends Filter {

	private final List<String> spatialHashCellsIds;
	private final String fieldName;

	public SpatialHashFilter(List<String> spatialHashCellsIds, String fieldName) {
		this.spatialHashCellsIds = spatialHashCellsIds;
		this.fieldName = fieldName;
	}

	/**
	 * Search the index for document having the correct spatial hash cell id at given grid level.
	 *
	 * @param context the {@link LeafReaderContext} for which to return the {@link DocIdSet}.
	 * @param acceptDocs Bits that represent the allowable docs to match (typically deleted docs but possibly filtering
	 * other documents)
	 * @return a {@link DocIdSet} with the document ids matching
	 */
	@Override
	public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs) throws IOException {
		if ( spatialHashCellsIds.size() == 0 ) {
			return null;
		}

		final LeafReader atomicReader = context.reader();

		BitDocIdSet matchedDocumentsIds = new BitDocIdSet( new FixedBitSet( atomicReader.maxDoc() ) );
		Boolean found = false;
		for ( int i = 0; i < spatialHashCellsIds.size(); i++ ) {
			Term spatialHashCellTerm = new Term( fieldName, spatialHashCellsIds.get( i ) );
			DocsEnum spatialHashCellsDocs = atomicReader.termDocsEnum( spatialHashCellTerm );
			if ( spatialHashCellsDocs != null ) {
				while ( true ) {
					final int docId = spatialHashCellsDocs.nextDoc();
					if ( docId == DocIdSetIterator.NO_MORE_DOCS ) {
						break;
					}
					else {
						if ( acceptDocs == null || acceptDocs.get( docId ) ) {
							matchedDocumentsIds.bits().set( docId );
							found = true;
						}
					}
				}
			}
		}

		if ( found ) {
			return matchedDocumentsIds;
		}
		else {
			return null;
		}
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
