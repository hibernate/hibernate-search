/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.OffsetTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneOffsetTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class LuceneOffsetTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneOffsetTimeIndexFieldTypeOptionsStep, OffsetTime> {

	LuceneOffsetTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetTime.class, DefaultStringConverters.OFFSET_TIME );
	}

	@Override
	protected LuceneOffsetTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<OffsetTime, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, OffsetTime indexNullAsValue) {
		return new LuceneOffsetTimeFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
