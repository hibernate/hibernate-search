/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneIntegerDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneBooleanFieldCodec extends AbstractLuceneNumericFieldCodec<Boolean, Integer> {

	public LuceneBooleanFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			Boolean indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, Boolean value,
			Integer encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Boolean decode(IndexableField field) {
		Integer intValue = (Integer) field.numericValue();
		return ( intValue > 0 );
	}

	@Override
	public Integer encode(Boolean value) {
		return value ? 1 : 0;
	}

	@Override
	public Boolean decode(Integer encoded) {
		return encoded > 0;
	}

	@Override
	public LuceneNumericDomain<Integer> getDomain() {
		return LuceneIntegerDomain.get();
	}

}
