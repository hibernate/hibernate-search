/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter.runtime;

import org.hibernate.search.util.common.SearchException;

/**
 * @deprecated Use {@link org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter}
 * with {@link FromDocumentValueConvertContext} instead.
 */
@Deprecated
public interface FromDocumentFieldValueConvertContext {

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering more options.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	<T> T extension(FromDocumentFieldValueConvertContextExtension<T> extension);

}
