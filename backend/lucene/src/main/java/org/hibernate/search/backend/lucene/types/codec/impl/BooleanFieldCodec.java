/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class BooleanFieldCodec implements LuceneFieldCodec<Boolean> {

	private final boolean projectable;

	private final boolean sortable;

	public BooleanFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Boolean value) {
		if ( value == null ) {
			return;
		}
		Integer intValue = ( value ) ? 1 : 0;

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, intValue ) );
		}

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, intValue.longValue() ) );
		}

		documentBuilder.addField( new IntPoint( absoluteFieldPath, intValue ) );
	}

	@Override
	public Boolean decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		Integer intValue = (Integer) field.numericValue();
		return ( intValue > 0 );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( BooleanFieldCodec.class != obj.getClass() ) {
			return false;
		}

		BooleanFieldCodec other = (BooleanFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable );
	}
}
