/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneIntegerDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneShortFieldCodec extends AbstractLuceneNumericFieldCodec<Short, Integer> {

	public LuceneShortFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			Short indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, Short value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Short decode(IndexableField field) {
		Integer integer = (Integer) field.numericValue();
		return integer.shortValue();
	}

	@Override
	public Integer encode(Short value) {
		return (int) value;
	}

	@Override
	public Short decode(Integer encoded) {
		return encoded.shortValue();
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneIntegerDomain.get();
	}
}
