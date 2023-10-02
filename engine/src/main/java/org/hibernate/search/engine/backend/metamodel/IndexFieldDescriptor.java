/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.metamodel;

/**
 * A field in the index.
 *
 * @see IndexValueFieldDescriptor
 * @see IndexObjectFieldDescriptor
 */
public interface IndexFieldDescriptor {

	/**
	 * @return {@code true} if this field is an object field.
	 * In that case, {@link #toObjectField()} can be called safely (it won't throw an exception).
	 */
	boolean isObjectField();

	/**
	 * @return {@code true} if this field is a value field.
	 * In that case, {@link #toValueField()} can be called safely (it won't throw an exception).
	 */
	boolean isValueField();

	/**
	 * @return This field as an {@link IndexObjectFieldDescriptor}, if possible. Never {@code null}.
	 * @throws org.hibernate.search.util.common.SearchException If this field is not an object field.
	 */
	IndexObjectFieldDescriptor toObjectField();

	/**
	 * @return This field as an {@link IndexValueFieldDescriptor}, if possible. Never {@code null}.
	 * @throws org.hibernate.search.util.common.SearchException If this field is not a value field.
	 */
	IndexValueFieldDescriptor toValueField();

	/**
	 * @return The type of this field, exposing its various capabilities.
	 * @see IndexFieldTypeDescriptor
	 */
	IndexFieldTypeDescriptor type();

	/**
	 * @return The parent of this field, either the {@link IndexCompositeElementDescriptor#isRoot() index root}
	 * or an {@link IndexCompositeElementDescriptor#isObjectField() object field}.
	 */
	IndexCompositeElementDescriptor parent();

	/**
	 * @return The name of this field relative to its {@link #parent() parent}.
	 */
	String relativeName();

	/**
	 * @return The absolute, dot-separated path of this field.
	 */
	String absolutePath();

	/**
	 * @return {@code true} if this field can have multiple values in the same parent document.
	 */
	boolean multiValued();

	/**
	 * @return {@code true} if this field can have multiple values in the same root document
	 * or if it is contained, directly or indirectly, in an object field that can have multiple values.
	 */
	boolean multiValuedInRoot();

}
