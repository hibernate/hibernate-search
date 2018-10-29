/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Locale;
import java.util.Objects;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;

public final class LocalDateFieldCodec implements LuceneFieldCodec<LocalDate> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private final Projectable projectable;

	private final Sortable sortable;

	public LocalDateFieldCodec(Projectable projectable, Sortable sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, LocalDate value) {
		if ( value == null ) {
			return;
		}

		switch ( projectable ) {
			case DEFAULT:
			case NO:
				break;
			case YES:
				documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
				break;
		}

		long valueToEpochDay = value.toEpochDay();

		switch ( sortable ) {
			case DEFAULT:
			case NO:
				break;
			case YES:
				documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, valueToEpochDay ) );
				break;
		}

		documentBuilder.addField( new LongPoint( absoluteFieldPath, valueToEpochDay ) );
	}

	@Override
	public LocalDate decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return LocalDate.parse( value, FORMATTER );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LocalDateFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LocalDateFieldCodec other = (LocalDateFieldCodec) obj;

		return Objects.equals( projectable, other.projectable ) && Objects.equals( sortable, other.sortable );
	}
}
