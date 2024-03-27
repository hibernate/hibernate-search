/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalDateTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalDateTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneLocalDateTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneLocalDateTimeIndexFieldTypeOptionsStep, LocalDateTime> {

	LuceneLocalDateTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDateTime.class );
	}

	@Override
	protected LuceneLocalDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<LocalDateTime, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, LocalDateTime indexNullAsValue) {
		return new LuceneLocalDateTimeFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
