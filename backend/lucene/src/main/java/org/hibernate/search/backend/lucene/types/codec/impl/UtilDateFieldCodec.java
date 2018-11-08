/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Date;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class UtilDateFieldCodec implements LuceneFieldCodec<Date> {

	private final boolean projectable;

	private final boolean sortable;

	public UtilDateFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Date value) {
		if ( value == null ) {
			return;
		}

		long time = value.getTime();
		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, time ) );
		}

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, time ) );
		}

		documentBuilder.addField( new LongPoint( absoluteFieldPath, time ) );
	}

	@Override
	public Date decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		Long time = (Long) field.numericValue();
		return new Date( time );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( UtilDateFieldCodec.class != obj.getClass() ) {
			return false;
		}

		UtilDateFieldCodec other = (UtilDateFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable );
	}
}
