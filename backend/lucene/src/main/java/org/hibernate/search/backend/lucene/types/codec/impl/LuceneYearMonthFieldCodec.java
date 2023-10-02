/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneYearMonthFieldCodec extends AbstractLuceneNumericFieldCodec<YearMonth, Long> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	/**
	 * Some YearMonth whose value does not matter, to be able to generate a YearMonth from a proleptic month.
	 */
	private static final YearMonth SOME_YEAR_MONTH = YearMonth.of( 0, 1 );

	public LuceneYearMonthFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			YearMonth indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, YearMonth value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
	}

	@Override
	public YearMonth decode(IndexableField field) {
		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return YearMonth.parse( value, FORMATTER );
	}

	@Override
	public Long encode(YearMonth value) {
		return value == null ? null : value.getLong( ChronoField.PROLEPTIC_MONTH );
	}

	@Override
	public YearMonth decode(Long encoded) {
		return SOME_YEAR_MONTH.with( ChronoField.PROLEPTIC_MONTH, encoded );
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}
}
