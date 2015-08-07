/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl.nullencoding;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * @author Sanne Grinovero
 */
public class KeywordBasedNullCodec implements NullMarkerCodec {

	private final String indexNullAs;
	private final BytesRef encodedToken;

	public KeywordBasedNullCodec(final String indexNullAs) {
		if ( indexNullAs == null ) {
			throw new NullPointerException( "The constructor parameter is mandatory" );
		}
		this.indexNullAs = indexNullAs;
		this.encodedToken = new BytesRef( indexNullAs );
	}

	@Override
	public String nullRepresentedAsString() {
		return indexNullAs;
	}

	@Override
	public void encodeNullValue(String name, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addFieldToDocument( name, indexNullAs, document );
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		return new TermQuery( new Term( fieldName, encodedToken ) );
	}

	@Override
	public boolean representsNullValue(IndexableField field) {
		String stringValue = field.stringValue();
		return indexNullAs.equals( stringValue );
	}

}
