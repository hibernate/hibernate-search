/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial and final step in a "vector" index field type definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
@Incubating
public interface VectorFieldTypeOptionsStep<S extends VectorFieldTypeOptionsStep<?, F>, F>
		extends SearchableProjectableIndexFieldTypeOptionsStep<S, F> {

	/**
	 * @param vectorSimilarity Defines how vector similarity is calculated.
	 * @return {@code this}, for method chaining.
	 */
	S vectorSimilarity(VectorSimilarity vectorSimilarity);

	/**
	 * @param efConstruction Defines the size of the dynamic list used during k-NN graph creation.
	 * @return {@code this}, for method chaining.
	 */
	S efConstruction(int efConstruction);

	/**
	 * @param m Defines the number of neighbors each node will be connected to in the HNSW graph.
	 * @return {@code this}, for method chaining.
	 */
	S m(int m);

	/**
	 * @param dimension The number of dimensions (array length) of vectors to be indexed.
	 * @return {@code this}, for method chaining.
	 */
	S dimension(int dimension);

}
