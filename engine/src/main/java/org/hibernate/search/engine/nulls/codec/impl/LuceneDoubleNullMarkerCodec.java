/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nulls.codec.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.spi.NullMarker;

/**
 * @author Sanne Grinovero
 */
public class LuceneDoubleNullMarkerCodec extends LuceneNullMarkerCodec {

	public LuceneDoubleNullMarkerCodec(NullMarker nullMarker) {
		super( nullMarker );
	}

	@Override
	public void encodeNullValue(String name, Document document, LuceneOptions luceneOptions) {
		luceneOptions.addNumericFieldToDocument( name, nullMarker.nullEncoded(), document );
	}

	@Override
	public Query createNullMatchingQuery(String fieldName) {
		Double nullEncoded = (Double) nullMarker.nullEncoded();
		return NumericRangeQuery.newDoubleRange( fieldName, nullEncoded, nullEncoded, true, true );
	}

	@Override
	public boolean representsNullValue(IndexableField field) {
		Number numericValue = field.numericValue();
		return nullMarker.nullEncoded().equals( numericValue );
	}

}
