/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

/**
 * The type of a field in the index.
 * <p>
 * Used when defining the index schema.
 *
 * @param <F> The Java type of values held by fields.
 *
 * @see org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement#field(String, IndexFieldType)
 */
public interface IndexFieldType<F> {
}
