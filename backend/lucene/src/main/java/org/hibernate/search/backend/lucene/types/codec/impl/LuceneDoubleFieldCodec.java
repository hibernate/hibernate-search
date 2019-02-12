/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneDoubleFieldCodec implements LuceneNumericFieldCodec<Double, Double> {

	private final boolean projectable;

	private final boolean sortable;

	public LuceneDoubleFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Double value) {
		if ( value == null ) {
			return;
		}

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, value ) );
		}

		if ( sortable ) {
			documentBuilder.addField( new DoubleDocValuesField( absoluteFieldPath, value ) );
		}

		documentBuilder.addField( new DoublePoint( absoluteFieldPath, value ) );
	}

	@Override
	public Double decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return (Double) field.numericValue();
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneDoubleFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneDoubleFieldCodec other = (LuceneDoubleFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable );
	}

	@Override
	public Double encode(Double value) {
		return value;
	}

	@Override
	public LuceneNumericDomain<Double> getDomain() {
		return LuceneNumericDomain.DOUBLE;
	}
}
