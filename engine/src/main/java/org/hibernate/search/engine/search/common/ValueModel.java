/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Defines the expectations on how the values passed to the Search DSL and returned back are represented.
 */
public enum ValueModel {

	/**
	 * This is the default model that allows working with the types as defined on the entity side.
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
	MAPPING,
	/**
	 * This model does not apply conversion and allows working with the types as defined on the index side.
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
	INDEX,
	/**
	 * This model applies formatting and parsing allowing working with the string representation of values.
	 * <p>
	 * For string values passed to the DSL (for example the parameter of a match predicate),
	 * the {@link IndexFieldTypeConverterStep#parser(ToDocumentValueConverter) parser converter}
	 * defined in the mapping will be used.
	 * This generally means strings passed to the DSL will be expected to be formatted in a way that
	 * parsing of them can be done in the same way as {@link org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep#indexNullAs(Object) indexing-null-as}
	 * parsing is performed.
	 * <p>
	 * For identifier values passed to the DSL (for example the parameter of an ID predicate),
	 * the identifier parser converter defined in the mapping will be used.
	 * <p>
	 * For fields values returned by the backend (for example in projections),
	 * the {@link IndexFieldTypeConverterStep#formatter(FromDocumentValueConverter)} formatter}
	 * defined in the mapping will be used.
	 * This generally means the projected values will be strings that can be parsed back via
	 * the {@link IndexFieldTypeConverterStep#parser(ToDocumentValueConverter) parser converter}.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	@Incubating
	STRING,
	/**
	 * This model does not apply conversion and allows working with the types that the backend operates with on a low level.
	 * <p>
	 * The values are passed straight to the backend as is. Conversion is disabled. Any encoding and decoding is also disabled.
	 * <p>
	 * For field values passed to the DSL (for example the parameter of a match predicate),
	 * no converter will be used.
	 * This generally means values passed to the DSL will be expected to have the same type as the type that backend uses to represent the index field.
	 * <p>
	 * For identifier values passed to the DSL (for example the parameter of an ID predicate),
	 * no converter will be used.
	 * This means values passed to the DSL will be expected to be strings that match document identifiers exactly.
	 * <p>
	 * For fields values returned by the backend (for example in projections),
	 * no converter will be used.
	 * This generally means the projected values will have the same type as the type that backend uses to represent the index field.
	 * <p>
	 * Requesting a raw value model representation requires working with backend specific types
	 * which are dependent on a particular backend implementation, hence these types are not guaranteed to be stable,
	 * and may change in the future.
	 * <p>
	 * Please refer to the reference documentation for more information.
	 */
	@Incubating
	RAW,
	/**
	 * The result of applying this constant is the same as {@link #MAPPING}.
	 * <p>
	 * The constant is only available to ease migration from the {@link ValueConvert} enum, and will be removed in the future.
	 *
	 * @deprecated This constant is deprecated and will be removed at the same time as the {@link ValueConvert} enum is removed.
	 * Do <b>not</b> use this value explicitly.
	 */
	@Deprecated(since = "7.2", forRemoval = true)
	DEFAULT
}
