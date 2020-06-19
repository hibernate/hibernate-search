/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;

public abstract class AbstractLuceneSort implements LuceneSearchSort {

	private final Set<String> indexNames;

	protected AbstractLuceneSort(AbstractBuilder builder) {
		this( builder.searchContext );
	}

	protected AbstractLuceneSort(LuceneSearchContext searchContext) {
		indexNames = searchContext.indexes().indexNames();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder implements SearchSortBuilder {
		protected final LuceneSearchContext searchContext;

		protected AbstractBuilder(LuceneSearchContext searchContext) {
			this.searchContext = searchContext;
		}
	}
}
