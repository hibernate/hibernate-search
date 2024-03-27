/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneDoubleFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneDoubleIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneDoubleIndexFieldTypeOptionsStep, Double> {

	LuceneDoubleIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Double.class );
	}

	@Override
	protected LuceneDoubleIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Double, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Double indexNullAsValue) {
		return new LuceneDoubleFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
