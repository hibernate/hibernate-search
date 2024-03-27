/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.dsl;


/**
 * An extension to the index field type DSL, allowing to create non-standard types in an index schema.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended index field type factories.
 * Should generally extend {@link IndexFieldTypeFactory}.
 *
 * @see IndexFieldTypeFactory#extension(IndexFieldTypeFactoryExtension)
 */
public interface IndexFieldTypeFactoryExtension<T> {

	/**
	 * Attempt to extend a given factory, throwing an exception in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link IndexFieldTypeFactory}.
	 * @return An extended index field type factory ({@link T})
	 */
	T extendOrFail(IndexFieldTypeFactory original);

}
