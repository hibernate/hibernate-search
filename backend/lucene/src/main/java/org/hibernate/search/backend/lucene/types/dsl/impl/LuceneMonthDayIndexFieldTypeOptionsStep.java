/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.MonthDay;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneMonthDayFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneMonthDayIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneMonthDayIndexFieldTypeOptionsStep, MonthDay> {

	LuceneMonthDayIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, MonthDay.class );
	}

	@Override
	protected LuceneMonthDayIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<MonthDay, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, MonthDay indexNullAsValue) {
		return new LuceneMonthDayFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
