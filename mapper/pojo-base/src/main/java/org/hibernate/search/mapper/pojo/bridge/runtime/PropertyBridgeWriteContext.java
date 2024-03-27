/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.util.common.SearchException;

/**
 * The context passed to
 * {@link org.hibernate.search.mapper.pojo.bridge.PropertyBridge#write(DocumentElement, Object, PropertyBridgeWriteContext)}.
 */
public interface PropertyBridgeWriteContext {

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering more options.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	<T> T extension(PropertyBridgeWriteContextExtension<T> extension);

}
