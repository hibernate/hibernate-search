/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.spatial;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.util.List;

/**
 * Lucene Filter for filtering documents which have been indexed with Hibernate Search Spatial SpatialFieldBridge
 * Use denormalized Grid Cell Ids to return a sub set of documents near the center
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @see SpatialFieldBridge
 * @see SpatialIndexable
 */
final class GridFilter extends Filter {

	private final List<String> gridCellsIds;
	private final String fieldName;

	public GridFilter(List<String> gridCellsIds, String fieldName) {
		this.gridCellsIds = gridCellsIds;
		this.fieldName = fieldName;
	}

	/**
	 * Returns Doc Ids by searching the index for document having the correct Grid Cell Id at given grid level
	 *
	 * @param reader reader to the index
	 */
	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		if ( gridCellsIds.size() == 0 ) {
			return null;
		}

		OpenBitSet matchedDocumentsIds = new OpenBitSet( reader.maxDoc() );
		Boolean found = false;
		for ( int i = 0; i < gridCellsIds.size(); i++ ) {
			Term gridCellTerm = new Term( fieldName, gridCellsIds.get( i ) );
			TermDocs gridCellsDocs = reader.termDocs( gridCellTerm );
			if ( gridCellsDocs != null ) {
				while ( gridCellsDocs.next() ) {
					matchedDocumentsIds.fastSet( gridCellsDocs.doc() );
					found = true;
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
}