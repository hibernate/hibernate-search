/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.Year;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneYearFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneYearIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneYearIndexFieldTypeOptionsStep, Year> {

	LuceneYearIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Year.class );
	}

	@Override
	protected LuceneYearIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Year, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Year indexNullAsValue) {
		return new LuceneYearFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
