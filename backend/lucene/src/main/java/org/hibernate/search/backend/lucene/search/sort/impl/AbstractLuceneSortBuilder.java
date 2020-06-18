/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;

public abstract class AbstractLuceneSortBuilder implements SearchSortBuilder, LuceneSearchSortBuilder {

	protected final LuceneSearchContext searchContext;

	protected AbstractLuceneSortBuilder(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
	}

	@Override
	public final SearchSort build() {
		// TODO HSEARCH-3476 this is just a temporary hack:
		//  we should move to one SearchSort implementation per type of sort
		return LuceneSearchSort.of( searchContext, this );
	}
}
