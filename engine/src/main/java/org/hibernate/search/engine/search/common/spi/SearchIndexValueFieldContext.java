/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

/**
 * Information about a value (non-object) field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <SC> The type of the backend-specific search scope.
 */
public interface SearchIndexValueFieldContext<SC extends SearchIndexScope<?>>
		extends SearchIndexNodeContext<SC> {

	SearchIndexValueFieldTypeContext<SC, ?, ?> type();

}
