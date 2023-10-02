/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneLocalDateTimeFieldCodec extends AbstractLuceneNumericFieldCodec<LocalDateTime, Long> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LuceneLocalDateFieldCodec.FORMATTER )
			.appendLiteral( 'T' )
			.append( LuceneLocalTimeFieldCodec.FORMATTER )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public LuceneLocalDateTimeFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			LocalDateTime indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, LocalDateTime value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
	}

	@Override
	public LocalDateTime decode(IndexableField field) {
		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return LocalDateTime.parse( value, FORMATTER );
	}

	@Override
	public Long encode(LocalDateTime value) {
		return value == null ? null : value.toInstant( ZoneOffset.UTC ).toEpochMilli();
	}

	@Override
	public LocalDateTime decode(Long encoded) {
		return Instant.ofEpochMilli( encoded ).atOffset( ZoneOffset.UTC ).toLocalDateTime();
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}
}
