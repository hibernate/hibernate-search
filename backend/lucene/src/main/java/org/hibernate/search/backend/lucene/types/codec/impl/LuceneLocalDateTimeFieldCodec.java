/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneLocalDateTimeFieldCodec implements LuceneNumericFieldCodec<LocalDateTime, Long> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LuceneLocalDateFieldCodec.FORMATTER )
			.appendLiteral( 'T' )
			.appendValue( HOUR_OF_DAY, 2 )
			.appendLiteral( ':' )
			.appendValue( MINUTE_OF_HOUR, 2 )
			.optionalStart()
			.appendLiteral( ':' )
			.appendValue( SECOND_OF_MINUTE, 2 )
			.optionalStart()
			.appendFraction( NANO_OF_SECOND, 3, 9, true )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private final boolean projectable;

	private final boolean sortable;

	public LuceneLocalDateTimeFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, LocalDateTime value) {
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
	public LocalDateTime decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return LocalDateTime.parse( value, FORMATTER );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneLocalDateTimeFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneLocalDateTimeFieldCodec other = (LuceneLocalDateTimeFieldCodec) obj;

		return ( projectable == other.projectable ) && ( sortable == other.sortable );
	}

	@Override
	public Long encode(LocalDateTime value) {
		return value == null ? null : value.atZone( ZoneOffset.UTC ).toInstant().toEpochMilli();
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneNumericDomain.LONG;
	}
}
