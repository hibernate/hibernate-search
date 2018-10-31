/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.util.List;

import org.apache.lucene.search.SortField;


/**
 * A sort collector for Lucene, using {@link SortField} to represent sorts.
 * <p>
 * Used by Lucene-specific sort contributors.
 *
 * @see SearchSortBuilderFactoryImpl#contribute(Object, List)
 * @see LuceneSearchSortBuilder
 */
public interface LuceneSearchSortCollector {

	void collectSortField(SortField sortField);

	void collectSortFields(SortField[] sortFields);

}
