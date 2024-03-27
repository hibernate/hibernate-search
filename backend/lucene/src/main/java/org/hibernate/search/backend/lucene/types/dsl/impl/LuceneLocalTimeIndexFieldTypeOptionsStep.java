/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneLocalTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneLocalTimeIndexFieldTypeOptionsStep, LocalTime> {

	LuceneLocalTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalTime.class );
	}

	@Override
	protected LuceneLocalTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<LocalTime, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, LocalTime indexNullAsValue) {
		return new LuceneLocalTimeFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
