/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.List;

import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about a field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 */
public interface LuceneSearchFieldContext extends EventContextProvider {

	boolean isObjectField();

	LuceneSearchObjectFieldContext toObjectField();

	String absolutePath();

	List<String> nestedPathHierarchy();

	// Query elements: predicates, sorts, projections, aggregations, ...

	<T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchContext searchContext);

}
