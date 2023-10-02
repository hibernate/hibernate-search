/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.metamodel;

/**
 * A "value" field in the index, i.e. a field that holds a string, integer, etc.
 * <p>
 * "value", in this context, is opposed to "object", as in {@link IndexObjectFieldDescriptor object field}.
 */
public interface IndexValueFieldDescriptor extends IndexFieldDescriptor {

	/**
	 * @return The type of this field, exposing its various capabilities and accepted Java types.
	 * @see IndexValueFieldTypeDescriptor
	 */
	IndexValueFieldTypeDescriptor type();

}
