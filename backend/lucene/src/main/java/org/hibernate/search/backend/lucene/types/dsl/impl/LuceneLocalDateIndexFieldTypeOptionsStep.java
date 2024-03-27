/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalDate;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalDateFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneLocalDateIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneLocalDateIndexFieldTypeOptionsStep, LocalDate> {

	LuceneLocalDateIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDate.class );
	}

	@Override
	protected LuceneLocalDateIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<LocalDate, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, LocalDate indexNullAsValue) {
		return new LuceneLocalDateFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
