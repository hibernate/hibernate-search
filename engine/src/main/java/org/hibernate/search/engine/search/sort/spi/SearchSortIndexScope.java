/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.search.common.spi.SearchIndexScope;

public interface SearchSortIndexScope<S extends SearchSortIndexScope<?>>
		extends SearchIndexScope<S> {

	SearchSortBuilderFactory sortBuilders();

}
