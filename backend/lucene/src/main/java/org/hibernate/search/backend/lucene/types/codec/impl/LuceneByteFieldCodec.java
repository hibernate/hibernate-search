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

public final class LuceneByteFieldCodec extends AbstractLuceneNumericFieldCodec<Byte, Integer> {

	public LuceneByteFieldCodec(boolean projectable, boolean sortable) {
		super( projectable, sortable );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Byte value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Byte decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		Integer integer = (Integer) field.numericValue();
		return integer.byteValue();
	}

	@Override
	public Integer encode(Byte value) {
		return (int) value;
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneNumericDomain.INTEGER;
	}
}
