/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.YearMonth;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneYearMonthFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneYearMonthIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneYearMonthIndexFieldTypeOptionsStep, YearMonth> {

	LuceneYearMonthIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class );
	}

	@Override
	protected LuceneYearMonthIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<YearMonth, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, YearMonth indexNullAsValue) {
		return new LuceneYearMonthFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
