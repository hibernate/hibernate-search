/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;

public abstract class AbstractLuceneSearchSortBuilder implements SearchSortBuilder<LuceneSearchSortBuilder>,
		LuceneSearchSortBuilder {

	protected SortOrder order;

	protected SortMode multi;

	@Override
	public LuceneSearchSortBuilder toImplementation() {
		return this;
	}

	@Override
	public void order(SortOrder order) {
		this.order = order;
	}

	@Override
	public void mode(SortMode multi) {
		this.multi = multi;
	}

}
