/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneByteFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneByteIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneByteIndexFieldTypeOptionsStep, Byte> {

	LuceneByteIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Byte.class );
	}

	@Override
	protected LuceneByteIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Byte, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Byte indexNullAsValue) {
		return new LuceneByteFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
