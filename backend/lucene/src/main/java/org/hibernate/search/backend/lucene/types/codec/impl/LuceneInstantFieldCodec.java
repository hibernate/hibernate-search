/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Instant;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneInstantFieldCodec implements LuceneFieldCodec<Instant> {

	private final boolean projectable;

	private final boolean sortable;

	public LuceneInstantFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, Instant value) {
		if ( value == null ) {
			return;
		}

		long time = value.toEpochMilli();
		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, time ) );
		}

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, time ) );
		}

		documentBuilder.addField( new LongPoint( absoluteFieldPath, time ) );
	}

	@Override
	public Instant decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		Long time = (Long) field.numericValue();
		return Instant.ofEpochMilli( time );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneInstantFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneInstantFieldCodec other = (LuceneInstantFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable );
	}
}
