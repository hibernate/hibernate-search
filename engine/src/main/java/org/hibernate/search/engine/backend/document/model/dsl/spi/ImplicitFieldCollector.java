/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ImplicitFieldCollector {

	/**
	 * Returned factory can be used to easily create a field type for {@link #addImplicitField(String, IndexFieldType)}.
	 *
	 * @return A factory for types of index fields.
	 */
	IndexFieldTypeFactory indexFieldTypeFactory();

	/**
	 * Create a new implicit field. It is expected that the field will be present in the created index. Mapping will not
	 * be modified.
	 *
	 * @param fieldName The relative name of the field
	 * @param indexFieldType The type of the field
	 * @param <F> The type of values for the field
	 */
	<F> void addImplicitField(String fieldName, IndexFieldType<F> indexFieldType);
}
