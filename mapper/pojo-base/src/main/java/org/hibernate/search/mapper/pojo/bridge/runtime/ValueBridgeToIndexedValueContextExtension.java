/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;

/**
 * An extension to {@link ValueBridgeToIndexedValueContext}, allowing to access non-standard context
 * specific to a given mapper.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended contexts.
 *
 * @see ValueBridgeToIndexedValueContext#extension(ValueBridgeToIndexedValueContextExtension)
 */
public interface ValueBridgeToIndexedValueContextExtension<T> {

	/**
	 * Attempt to extend a given context, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link ValueBridgeToIndexedValueContext}.
	 * @param mappingContext A {@link BridgeMappingContext}.
	 * @return An optional containing the extended context ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(ValueBridgeToIndexedValueContext original,
			BridgeMappingContext mappingContext);

}
