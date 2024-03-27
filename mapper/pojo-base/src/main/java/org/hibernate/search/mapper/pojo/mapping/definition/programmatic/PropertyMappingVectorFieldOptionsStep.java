/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a vector field.
 *
 * @see VectorField
 */
@Incubating
public interface PropertyMappingVectorFieldOptionsStep
		extends PropertyMappingFieldOptionsStep<PropertyMappingVectorFieldOptionsStep> {

	/**
	 * @param projectable Whether this field should be projectable.
	 * @return {@code this}, for method chaining.
	 * @see VectorField#projectable()
	 * @see Projectable
	 */
	PropertyMappingVectorFieldOptionsStep projectable(Projectable projectable);

	/**
	 * @param searchable Whether this field should be searchable.
	 * @return {@code this}, for method chaining.
	 * @see VectorField#searchable()
	 * @see Searchable
	 */
	PropertyMappingVectorFieldOptionsStep searchable(Searchable searchable);

	/**
	 * @param vectorSimilarity method of calculating the vector similarity, i.e. distance between vectors.
	 * @return {@code this}, for method chaining.
	 * @see VectorField#vectorSimilarity()
	 * @see VectorSimilarity
	 */
	PropertyMappingVectorFieldOptionsStep vectorSimilarity(VectorSimilarity vectorSimilarity);

	/**
	 * @param efConstruction The size of the dynamic list used during k-NN graph creation.
	 * @return {@code this}, for method chaining.
	 * @see VectorField#efConstruction()
	 */
	PropertyMappingVectorFieldOptionsStep efConstruction(int efConstruction);

	/**
	 * @param m The number of neighbors each node will be connected to in the HNSW graph.
	 * @return {@code this}, for method chaining.
	 * @see VectorField#m()
	 */
	PropertyMappingVectorFieldOptionsStep m(int m);

	/**
	 * @param indexNullAs A value used instead of null values when indexing.
	 * @return {@code this}, for method chaining.
	 * @see VectorField#indexNullAs()
	 */
	PropertyMappingVectorFieldOptionsStep indexNullAs(String indexNullAs);

}
