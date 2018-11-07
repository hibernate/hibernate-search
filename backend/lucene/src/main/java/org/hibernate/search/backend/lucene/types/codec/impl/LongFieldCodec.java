/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LongFieldCodec implements LuceneFieldCodec<Long> {

	private final boolean projectable;

	private final boolean sortable;

	public LongFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Long value) {
		if ( value == null ) {
			return;
		}

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, value ) );
		}

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, value.longValue() ) );
		}

		documentBuilder.addField( new LongPoint( absoluteFieldPath, value ) );
	}

	@Override
	public Long decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return (Long) field.numericValue();
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LongFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LongFieldCodec other = (LongFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable );
	}
}
