/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneFloatFieldCodec extends AbstractLuceneNumericFieldCodec<Float, Float> {

	public LuceneFloatFieldCodec(boolean projectable, boolean sortable) {
		super( projectable, sortable );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Float value,
			Float encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Float decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return (Float) field.numericValue();
	}

	@Override
	public Float encode(Float value) {
		return value;
	}

	@Override
	public LuceneNumericDomain<Float> getDomain() {
		return LuceneNumericDomain.FLOAT;
	}
}
