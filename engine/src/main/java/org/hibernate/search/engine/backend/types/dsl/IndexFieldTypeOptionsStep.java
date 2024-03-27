/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.dsl;

/**
 * The final step in an index field type definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
public interface IndexFieldTypeOptionsStep<S extends IndexFieldTypeOptionsStep<?, F>, F>
		extends IndexFieldTypeFinalStep<F>, IndexFieldTypeConverterStep<S, F> {

}
