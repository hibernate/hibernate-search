/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.OffsetDateTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneOffsetDateTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneOffsetDateTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneOffsetDateTimeIndexFieldTypeOptionsStep, OffsetDateTime> {

	LuceneOffsetDateTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetDateTime.class );
	}

	@Override
	protected LuceneOffsetDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<OffsetDateTime, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, OffsetDateTime indexNullAsValue) {
		return new LuceneOffsetDateTimeFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
