/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.Objects;

import org.hibernate.search.engine.backend.common.DocumentReference;



public class LuceneDocumentReference implements DocumentReference {

	private final String indexName;

	private final String id;

	public LuceneDocumentReference(String indexName, String id) {
		this.indexName = indexName;
		this.id = id;
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || obj.getClass() != getClass() ) {
			return false;
		}
		LuceneDocumentReference other = (LuceneDocumentReference) obj;
		return Objects.equals( indexName, other.indexName ) && Objects.equals( id, other.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( indexName, id );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexName=" ).append( indexName )
				.append( ", id=" ).append( id )
				.append( "]" )
				.toString();
	}

}
