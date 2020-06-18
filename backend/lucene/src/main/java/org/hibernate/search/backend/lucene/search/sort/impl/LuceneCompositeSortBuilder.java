/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;

class LuceneCompositeSortBuilder extends AbstractLuceneSortBuilder implements CompositeSortBuilder {

	private final List<LuceneSearchSort> elements = new ArrayList<>();

	LuceneCompositeSortBuilder(LuceneSearchContext searchContext) {
		super( searchContext );
	}

	@Override
	public void add(SearchSort sort) {
		elements.add( LuceneSearchSort.from( searchContext, sort ) );
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		for ( LuceneSearchSort element : elements ) {
			element.toSortFields( collector );
		}
	}
}
