/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneIntegerDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneIntegerFieldCodec extends AbstractLuceneNumericFieldCodec<Integer, Integer> {

	public LuceneIntegerFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			Integer indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, Integer value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Integer decode(IndexableField field) {
		return (Integer) field.numericValue();
	}

	@Override
	public Integer encode(Integer value) {
		return value;
	}

	@Override
	public Integer decode(Integer encoded) {
		return encoded;
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneIntegerDomain.get();
	}
}
