/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneIntegerFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneIntegerIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneIntegerIndexFieldTypeOptionsStep, Integer> {

	LuceneIntegerIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Integer.class );
	}

	@Override
	protected LuceneIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Integer, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Integer indexNullAsValue) {
		return new LuceneIntegerFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
