/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.apache.lucene.search.SortField;

/**
 * A sort collector for Lucene, using {@link SortField} to represent sorts.
 * <p>
 * Used by Lucene-specific sort contributors.
 *
 * @see LuceneSearchSort#toSortFields(org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector)
 */
public interface LuceneSearchSortCollector extends SortRequestContext {

	void collectSortField(SortField sortField);

	void collectSortFields(SortField[] sortFields);

}
