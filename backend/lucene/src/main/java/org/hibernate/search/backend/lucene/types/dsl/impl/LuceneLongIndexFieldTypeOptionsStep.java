/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLongFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

class LuceneLongIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneLongIndexFieldTypeOptionsStep, Long> {

	LuceneLongIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Long.class );
	}

	@Override
	protected LuceneLongIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Long, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Long indexNullAsValue) {
		return new LuceneLongFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
