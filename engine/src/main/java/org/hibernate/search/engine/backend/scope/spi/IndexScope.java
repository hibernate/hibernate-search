/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.scope.spi;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.util.common.SearchException;

/**
 * The scope of an index-related operation, aware of the targeted indexes and of the underlying technology (backend).
 */
public interface IndexScope {

	SearchQueryIndexScope<? extends SearchQueryIndexScope<?>> searchScope();

	/**
	 * Extend the current index scope with the given extension,
	 * resulting in an extended index scope offering backend-specific utilities.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of index scope provided by the extension.
	 * @return The extended index scope.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	default <T> T extension(IndexScopeExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
