/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.metamodel;

/**
 * An "object" field in the index, i.e. a field that holds other fields.
 */
public interface IndexObjectFieldDescriptor extends IndexFieldDescriptor, IndexCompositeElementDescriptor {

	/**
	 * @return The type of this field, exposing its various capabilities.
	 * @see IndexObjectFieldTypeDescriptor
	 */
	IndexObjectFieldTypeDescriptor type();

}
