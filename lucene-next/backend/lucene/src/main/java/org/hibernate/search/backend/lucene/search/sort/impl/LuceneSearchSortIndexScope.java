/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;

public interface LuceneSearchSortIndexScope<S extends LuceneSearchSortIndexScope<?>>
		extends SearchSortIndexScope<S>, LuceneSearchIndexScope<S> {

	@Override
	LuceneSearchSortBuilderFactory sortBuilders();

}
