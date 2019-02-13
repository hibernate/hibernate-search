/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneFloatFieldCodec implements LuceneNumericFieldCodec<Float, Float> {

	private final boolean projectable;

	private final boolean sortable;

	public LuceneFloatFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Float value) {
		if ( value == null ) {
			return;
		}

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, value ) );
		}

		if ( sortable ) {
			documentBuilder.addField( new FloatDocValuesField( absoluteFieldPath, value ) );
		}

		documentBuilder.addField( new FloatPoint( absoluteFieldPath, value ) );
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
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneFloatFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneFloatFieldCodec other = (LuceneFloatFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable );
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
