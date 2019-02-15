/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneMonthDayFieldCodec implements LuceneNumericFieldCodec<MonthDay, Integer> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendLiteral( "--" )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private final boolean projectable;

	private final boolean sortable;

	public LuceneMonthDayFieldCodec(boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, MonthDay value) {
		if ( value == null ) {
			return;
		}

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
		}

		Integer integerValue = encode( value );

		if ( sortable ) {
			documentBuilder.addField( new NumericDocValuesField( absoluteFieldPath, integerValue ) );
		}

		documentBuilder.addField( new IntPoint( absoluteFieldPath, integerValue ) );
	}

	@Override
	public MonthDay decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return MonthDay.parse( value, FORMATTER );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneMonthDayFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneMonthDayFieldCodec other = (LuceneMonthDayFieldCodec) obj;

		return ( projectable == other.projectable ) && ( sortable == other.sortable );
	}

	@Override
	public Integer encode(MonthDay value) {
		if ( value == null ) {
			return null;
		}

		return 100 * value.getMonthValue() + value.getDayOfMonth();
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneNumericDomain.INTEGER;
	}
}
