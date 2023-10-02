/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.index.VectorSimilarityFunction;

/**
 * Vector field specific codec that allows redefining {@link KnnVectorsFormat}.
 *
 * @param <F> The field type exposed to the mapper.
 */
public interface LuceneVectorFieldCodec<F> extends LuceneStandardFieldCodec<F, F> {

	/**
	 * Custom {@link KnnVectorsFormat knn vector format} that will be used in {@link org.apache.lucene.codecs.perfield.PerFieldKnnVectorsFormat}
	 * and can for example define custom {@code efConstruction} or {@code m} or even provide a completely custom implementation (needs to be registered via ServiceLoader mechanism).
	 */
	KnnVectorsFormat knnVectorFormat();

	/**
	 * @return The type of vector elements expected to get either {@code float.class} or {@code byte.class}.
	 */
	Class<?> vectorElementsType();

	/**
	 * @return The number of dimensions (array length) of vectors to be indexed that this codec can process.
	 */
	int getConfiguredDimensions();

	/**
	 * @return The vector similarity function used by this codec.
	 */
	VectorSimilarityFunction getVectorSimilarity();

}
