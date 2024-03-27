/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.Instant;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneInstantFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneInstantIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneInstantIndexFieldTypeOptionsStep, Instant> {

	LuceneInstantIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class );
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Instant, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Instant indexNullAsValue) {
		return new LuceneInstantFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	protected LuceneInstantIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
