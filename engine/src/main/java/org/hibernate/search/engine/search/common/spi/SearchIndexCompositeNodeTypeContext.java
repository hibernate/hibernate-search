/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

/**
 * Information about the type of a composite (non-value) index element targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <SC> The type of the backend-specific search scope.
 * @param <N> The type representing the targeted index node.
 */
public interface SearchIndexCompositeNodeTypeContext<
		SC extends SearchIndexScope<?>,
		N extends SearchIndexCompositeNodeContext<SC>>
		extends SearchIndexNodeTypeContext<SC, N> {

	boolean nested();

}
