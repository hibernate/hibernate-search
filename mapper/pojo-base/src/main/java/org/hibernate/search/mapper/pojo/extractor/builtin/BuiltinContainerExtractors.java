/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;

/**
 * The names of {@link ContainerExtractor container extractors} available in Hibernate Search out of the box.
 */
public class BuiltinContainerExtractors {

	private BuiltinContainerExtractors() {
	}

	/**
	 * The name of an extractor that extracts elements from an array of objects ({@code Object[]}, {@code Integer[]}, ...).
	 */
	public static final String ARRAY_OBJECT = "array-object";
	/**
	 * The name of an extractor that extracts elements from an array of primitive chars ({@code char[]}).
	 */
	public static final String ARRAY_CHAR = "array-char";
	/**
	 * The name of an extractor that extracts elements from an array of primitive booleans ({@code boolean[]}).
	 */
	public static final String ARRAY_BOOLEAN = "array-boolean";
	/**
	 * The name of an extractor that extracts elements from an array of primitive bytes ({@code byte[]}).
	 */
	public static final String ARRAY_BYTE = "array-byte";
	/**
	 * The name of an extractor that extracts elements from an array of primitive shorts ({@code short[]}).
	 */
	public static final String ARRAY_SHORT = "array-short";
	/**
	 * The name of an extractor that extracts elements from an array of primitive integers ({@code int[]}).
	 */
	public static final String ARRAY_INT = "array-int";
	/**
	 * The name of an extractor that extracts elements from an array of primitive longs ({@code long[]}).
	 */
	public static final String ARRAY_LONG = "array-long";
	/**
	 * The name of an extractor that extracts elements from an array of primitive floats ({@code float[]}).
	 */
	public static final String ARRAY_FLOAT = "array-float";
	/**
	 * The name of an extractor that extracts elements from an array of primitive double ({@code double[]}).
	 */
	public static final String ARRAY_DOUBLE = "array-double";
	/**
	 * The name of an extractor that extracts elements from a {@link java.util.Collection}.
	 */
	public static final String COLLECTION = "collection";
	/**
	 * The name of an extractor that extracts elements from an {@link Iterable}.
	 */
	public static final String ITERABLE = "iterable";
	/**
	 * The name of an extractor that extracts keys from a {@link java.util.Map}.
	 */
	public static final String MAP_KEY = "map-key";
	/**
	 * The name of an extractor that extracts values from a {@link java.util.Map}.
	 */
	public static final String MAP_VALUE = "map-value";
	/**
	 * The name of an extractor that extracts the value from an {@link java.util.OptionalDouble}.
	 */
	public static final String OPTIONAL_DOUBLE = "optional-double";
	/**
	 * The name of an extractor that extracts the value from an {@link java.util.OptionalInt}.
	 */
	public static final String OPTIONAL_INT = "optional-int";
	/**
	 * The name of an extractor that extracts the value from an {@link java.util.OptionalLong}.
	 */
	public static final String OPTIONAL_LONG = "optional-long";
	/**
	 * The name of an extractor that extracts the value from an {@link java.util.Optional}.
	 */
	public static final String OPTIONAL = "optional";

}
