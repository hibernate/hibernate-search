/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneInstantFieldCodec implements LuceneNumericFieldCodec<Instant, Long> {

	static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

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

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
		}

		long valueToEpochDay = encode( value );

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, valueToEpochDay ) );
		}

		documentBuilder.addField( new LongPoint( absoluteFieldPath, valueToEpochDay ) );
	}

	@Override
	public Instant decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return FORMATTER.parse( value, Instant::from );
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

		return ( projectable == other.projectable ) && ( sortable == other.sortable );
	}

	@Override
	public Long encode(Instant value) {
		return value == null ? null : value.toEpochMilli();
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneNumericDomain.LONG;
	}
}
