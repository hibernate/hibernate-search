/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Year;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneYearFieldCodec implements LuceneNumericFieldCodec<Year, Integer> {

	private final boolean projectable;

	private final boolean sortable;

	public LuceneYearFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Year value) {
		if ( value == null ) {
			return;
		}
		Integer intValue = encode( value );

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, intValue ) );
		}

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, intValue ) );
		}

		documentBuilder.addField( new IntPoint( absoluteFieldPath, intValue ) );
	}

	@Override
	public Year decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		Integer integer = (Integer) field.numericValue();
		return Year.of( integer );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneYearFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneYearFieldCodec other = (LuceneYearFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable );
	}

	@Override
	public Integer encode(Year value) {
		return value.getValue();
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneNumericDomain.INTEGER;
	}
}
