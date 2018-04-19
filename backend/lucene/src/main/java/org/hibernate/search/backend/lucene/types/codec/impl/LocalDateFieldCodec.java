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
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;

public final class LocalDateFieldCodec implements LuceneFieldCodec<LocalDate> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private final Store store;

	private final Sortable sortable;

	public LocalDateFieldCodec(Store store, Sortable sortable) {
		this.store = store;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, LuceneIndexSchemaObjectNode parentNode, String fieldName, LocalDate value) {
		if ( value == null ) {
			return;
		}

		if ( Store.YES.equals( store ) ) {
			documentBuilder.addField( parentNode, new StoredField( fieldName, FORMATTER.format( value ) ) );
		}

		long valueToEpochDay = value.toEpochDay();

		if ( Sortable.YES.equals( sortable ) ) {
			documentBuilder.addField( parentNode, new NumericDocValuesField( fieldName, valueToEpochDay ) );
		}

		documentBuilder.addField( parentNode, new LongPoint( fieldName, valueToEpochDay ) );
	}

	@Override
	public LocalDate decode(Document document, String fieldName) {
		IndexableField field = document.getField( fieldName );

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
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( LocalDateFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LocalDateFieldCodec other = (LocalDateFieldCodec) obj;

		return Objects.equals( store, other.store ) && Objects.equals( sortable, other.sortable );
	}

	@Override
	public int hashCode() {
		return Objects.hash( store, sortable );
	}
}
