/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneOffsetDateTimeFieldCodec extends AbstractLuceneNumericFieldCodec<OffsetDateTime, Long> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LuceneLocalDateTimeFieldCodec.FORMATTER )
			// OffsetId is mandatory
			.appendOffsetId()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public LuceneOffsetDateTimeFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			OffsetDateTime indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, OffsetDateTime value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
	}

	@Override
	public OffsetDateTime decode(IndexableField field) {
		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return OffsetDateTime.parse( value, FORMATTER );
	}

	@Override
	public Long encode(OffsetDateTime value) {
		return value == null ? null : value.toInstant().toEpochMilli();
	}

	@Override
	public OffsetDateTime decode(Long encoded) {
		return Instant.ofEpochMilli( encoded ).atOffset( ZoneOffset.UTC );
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}
}
