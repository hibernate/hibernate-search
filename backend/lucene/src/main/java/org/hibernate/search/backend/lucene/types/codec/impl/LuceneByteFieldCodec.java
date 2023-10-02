/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneIntegerDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneByteFieldCodec extends AbstractLuceneNumericFieldCodec<Byte, Integer> {

	public LuceneByteFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			Byte indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, Byte value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Byte decode(IndexableField field) {
		Integer integer = (Integer) field.numericValue();
		return integer.byteValue();
	}

	@Override
	public Integer encode(Byte value) {
		return (int) value;
	}

	@Override
	public Byte decode(Integer encoded) {
		return encoded.byteValue();
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneIntegerDomain.get();
	}
}
