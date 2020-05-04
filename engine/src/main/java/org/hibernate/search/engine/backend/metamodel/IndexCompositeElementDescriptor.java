/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.metamodel;

import java.util.Collection;
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
	 * @return A collection containing all fields.
	 */
	Collection<? extends IndexFieldDescriptor> staticChildren();

}
