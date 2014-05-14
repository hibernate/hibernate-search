/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.util.List;

/**
 * Lucene Filter for filtering documents which have been indexed with Hibernate Search Spatial SpatialFieldBridge
 * Use denormalized spatial hash cell ids to return a sub set of documents near the center
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
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
	 * Returns Doc Ids by searching the index for document having the correct spatial hash cell id at given grid level
	 *
	 * @param reader reader to the index
	 */
	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		if ( spatialHashCellsIds.size() == 0 ) {
			return null;
		}

		final AtomicReader atomicReader = context.reader();

		OpenBitSet matchedDocumentsIds = new OpenBitSet( atomicReader.maxDoc() );
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
							matchedDocumentsIds.fastSet( docId );
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
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "SpatialHashFilter" );
		sb.append( "{spatialHashCellsIds=" ).append( spatialHashCellsIds );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
