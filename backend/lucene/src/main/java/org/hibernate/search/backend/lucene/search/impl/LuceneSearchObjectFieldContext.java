/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.Map;

/**
 * Information about an object field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 */
public interface LuceneSearchObjectFieldContext extends LuceneSearchFieldContext {

	Map<String, ? extends LuceneSearchFieldContext> staticChildrenByName();

	<T> LuceneSearchObjectFieldQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key);

}
