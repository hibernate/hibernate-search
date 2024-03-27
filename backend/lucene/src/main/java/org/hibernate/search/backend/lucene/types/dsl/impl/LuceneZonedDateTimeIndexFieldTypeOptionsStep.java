/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.ZonedDateTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneZonedDateTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneZonedDateTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneZonedDateTimeIndexFieldTypeOptionsStep, ZonedDateTime> {

	LuceneZonedDateTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, ZonedDateTime.class );
	}

	@Override
	protected LuceneZonedDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<ZonedDateTime, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, ZonedDateTime indexNullAsValue) {
		return new LuceneZonedDateTimeFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
