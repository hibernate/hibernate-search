/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneVectorFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneByteVectorCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;

import org.apache.lucene.index.VectorSimilarityFunction;

class LuceneByteVectorFieldTypeOptionsStep
		extends AbstractLuceneVectorFieldTypeOptionsStep<LuceneByteVectorFieldTypeOptionsStep, byte[]> {

	LuceneByteVectorFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, byte[].class );
	}

	@Override
	protected LuceneByteVectorFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneVectorFieldCodec<byte[]> createCodec(VectorSimilarityFunction vectorSimilarity, int dimension,
			Storage storage, Indexing indexing, byte[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		return new LuceneByteVectorCodec( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
	}

}
