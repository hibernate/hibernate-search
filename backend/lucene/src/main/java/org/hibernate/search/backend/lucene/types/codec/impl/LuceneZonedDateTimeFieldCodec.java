/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.util.common.impl.TimeHelper;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneZonedDateTimeFieldCodec implements LuceneNumericFieldCodec<ZonedDateTime, Long> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LuceneOffsetDateTimeFieldCodec.FORMATTER )
			// ZoneRegionId is optional
			.optionalStart()
				.appendLiteral( '[' )
				.parseCaseSensitive()
				.appendZoneRegionId()
				.appendLiteral( ']' )
			.optionalEnd()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private final boolean projectable;

	private final boolean sortable;

	public LuceneZonedDateTimeFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, ZonedDateTime value) {
		if ( value == null ) {
			return;
		}

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
		}

		long numericValue = encode( value );

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, numericValue ) );
		}

		documentBuilder.addField( new LongPoint( absoluteFieldPath, numericValue ) );
	}

	@Override
	public ZonedDateTime decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return TimeHelper.parseZoneDateTime( value, FORMATTER );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneZonedDateTimeFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneZonedDateTimeFieldCodec other = (LuceneZonedDateTimeFieldCodec) obj;

		return ( projectable == other.projectable ) && ( sortable == other.sortable );
	}

	@Override
	public Long encode(ZonedDateTime value) {
		return value == null ? null : value.toInstant().toEpochMilli();
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneNumericDomain.LONG;
	}
}
