/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter.runtime;

import org.hibernate.search.util.common.SearchException;

/**
 * @deprecated Use {@link org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter}
 * with {@link ToDocumentValueConvertContext} instead.
 */
@Deprecated
public interface ToDocumentFieldValueConvertContext {

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering more options.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	<T> T extension(ToDocumentFieldValueConvertContextExtension<T> extension);

}
