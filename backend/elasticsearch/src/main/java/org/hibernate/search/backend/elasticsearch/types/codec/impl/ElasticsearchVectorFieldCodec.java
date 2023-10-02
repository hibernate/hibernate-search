/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

public interface ElasticsearchVectorFieldCodec<F> extends ElasticsearchFieldCodec<F> {

	/**
	 * @return The type of vector elements expected to get either {@code float.class} or {@code byte.class}.
	 */
	Class<?> vectorElementsType();

	/**
	 * @return The number of dimensions (array length) of vectors to be indexed that this codec can process.
	 */
	int getConfiguredDimensions();
}
