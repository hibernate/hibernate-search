/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is not a full-text field.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface PropertyMappingNonFullTextFieldOptionsStep<S extends PropertyMappingNonFullTextFieldOptionsStep<?>>
		extends PropertyMappingStandardFieldOptionsStep<S> {

	/**
	 * @param sortable Whether this field should be sortable.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#sortable()
	 * @see Sortable
	 */
	S sortable(Sortable sortable);

	/**
	 * @param aggregable Whether aggregations are enabled for this field.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#aggregable()
	 * @see Aggregable
	 */
	S aggregable(Aggregable aggregable);

	/**
	 * @param indexNullAs A value used instead of null values when indexing.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#indexNullAs()
	 */
	S indexNullAs(String indexNullAs);

}
