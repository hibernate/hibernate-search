/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.metamodel;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;

/**
 * A composite element in the index.
 * <p>
 * Composite elements are either the root or an {@link IndexObjectFieldDescriptor object field}.
 *
 * @see IndexObjectFieldDescriptor
 */
public interface IndexCompositeElementDescriptor {

	/**
	 * @return {@code true} if this element represents the root of the index.
	 */
	boolean isRoot();

	/**
	 * @return {@code true} if this element represents an object field.
	 * In that case, {@link #toObjectField()} can be called safely (it won't throw an exception).
	 */
	boolean isObjectField();

	/**
	 * @return This element as an {@link IndexObjectFieldDescriptor}, if possible. Never {@code null}.
	 * @throws org.hibernate.search.util.common.SearchException If this element does not represent an object field.
	 */
	IndexObjectFieldDescriptor toObjectField();

	/**
	 * Get all statically-defined, direct child fields for this element.
	 * <p>
	 * Only statically-defined fields are returned;
	 * fields created dynamically through {@link IndexSchemaElement#fieldTemplate(String, Function) templates}
	 * are not included in the collection.
	 *
	 * @return A collection containing all static child fields.
	 */
	Collection<? extends IndexFieldDescriptor> staticChildren();

	/**
	 * Get all statically-defined, direct child fields for this element,
	 * mapped by their {@link IndexFieldDescriptor#relativeName() relative name}.
	 * <p>
	 * Only statically-defined fields are returned;
	 * fields created dynamically through {@link IndexSchemaElement#fieldTemplate(String, Function) templates}
	 * are not included in the map.
	 *
	 * @return A map containing all static child fields.
	 */
	Map<String, ? extends IndexFieldDescriptor> staticChildrenByName();

}
