/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneInstantFieldCodec extends AbstractLuceneNumericFieldCodec<Instant, Long> {

	static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

	public LuceneInstantFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			Instant indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, Instant value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
	}

	@Override
	public Instant decode(IndexableField field) {
		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return FORMATTER.parse( value, Instant::from );
	}

	@Override
	public Long encode(Instant value) {
		return value == null ? null : value.toEpochMilli();
	}

	@Override
	public Instant decode(Long encoded) {
		return Instant.ofEpochMilli( encoded );
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}
}
