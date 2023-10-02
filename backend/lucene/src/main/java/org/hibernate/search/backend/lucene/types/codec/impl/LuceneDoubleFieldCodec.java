/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneDoubleDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneDoubleFieldCodec extends AbstractLuceneNumericFieldCodec<Double, Double> {

	public LuceneDoubleFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			Double indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, Double value,
			Double encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, encodedValue ) );
	}

	@Override
	public Double decode(IndexableField field) {
		return (Double) field.numericValue();
	}

	@Override
	public Double encode(Double value) {
		return value;
	}

	@Override
	public Double decode(Double encoded) {
		return encoded;
	}

	@Override
	public LuceneNumericDomain<Double> getDomain() {
		return LuceneDoubleDomain.get();
	}
}
