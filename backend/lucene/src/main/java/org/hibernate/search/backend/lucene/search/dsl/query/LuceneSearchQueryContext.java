/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query;

import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;

public interface LuceneSearchQueryContext<Q>
		extends SearchQueryContext<LuceneSearchQueryContext<Q>, Q, LuceneSearchSortContainerContext> {

	// FIXME make toQuery return a Lucene-specific query type

}
