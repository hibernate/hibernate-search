/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;

/**
 * Specifies whether values should be converted during search queries.
 */
public enum ValueConvert {

	/**
	 * Enables value conversion.
	 * <p>
	 * For field values passed to the DSL (for example the parameter of a match predicate),
	 * the {@link IndexFieldTypeConverterStep#dslConverter(Class, ToDocumentValueConverter) DSL converter}
	 * defined in the mapping will be used.
	 * This generally means values passed to the DSL will be expected to have the same type
	 * as the entity property used to populate the index field.
	 * <p>
	 * For identifier values passed to the DSL (for example the parameter of an ID predicate),
	 * the identifier converter defined in the mapping will be used.
	 * This generally means values passed to the DSL will be expected to have the same type
	 * as the entity property used to generate document identifiers.
	 * <p>
	 * For fields values returned by the backend (for example in projections),
	 * the {@link IndexFieldTypeConverterStep#projectionConverter(Class, FromDocumentValueConverter) projection converter}
	 * defined in the mapping will be used.
	 * This generally means the projected values will have the same type
	 * as the entity property used to populate the index field.
	 * <p>
	 * If no converter was defined in the mapping, this option won't have any effect.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	YES,
	/**
	 * Disables value conversion.
	 * <p>
	 * For field values passed to the DSL (for example the parameter of a match predicate),
	 * no converter will be used.
	 * This generally means values passed to the DSL will be expected to have the same type as the index field.
	 * <p>
	 * For identifier values passed to the DSL (for example the parameter of an ID predicate),
	 * no converter will be used.
	 * This means values passed to the DSL will be expected to be strings that match document identifiers exactly.
	 * <p>
	 * For fields values returned by the backend (for example in projections),
	 * no converter will be used.
	 * This generally means the projected values will have the same type as the index field.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	NO

}
