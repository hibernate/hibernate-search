/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.nullencoding.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.spi.NullMarkerCodec;

/**
 * @author Sanne Grinovero
 */
public class NumericDoubleNullCodec implements NullMarkerCodec {

	private final Double indexNullAs;

	public NumericDoubleNullCodec(final Double indexNullAs) throws NumberFormatException {
		if ( indexNullAs == null ) {
			throw new NullPointerException( "The constructor parameter is mandatory" );
		}
		this.indexNullAs = indexNullAs;
	}

	@Override
	public String nullRepresentedAsString() {
		return indexNullAs.toString();
	}

	@Override
	public void encodeNullValue(String name, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addNumericFieldToDocument( name, indexNullAs, document );
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		return NumericRangeQuery.newDoubleRange( fieldName, indexNullAs, indexNullAs, true, true );
	}

	@Override
	public boolean representsNullValue(IndexableField field) {
		Number numericValue = field.numericValue();
		return indexNullAs.equals( numericValue );
	}

}
