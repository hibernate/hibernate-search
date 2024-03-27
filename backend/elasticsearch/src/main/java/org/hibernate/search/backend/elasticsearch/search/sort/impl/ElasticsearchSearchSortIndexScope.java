/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;

public interface ElasticsearchSearchSortIndexScope<S extends ElasticsearchSearchSortIndexScope<?>>
		extends SearchSortIndexScope<S> {

	@Override
	ElasticsearchSearchSortBuilderFactory sortBuilders();

}
