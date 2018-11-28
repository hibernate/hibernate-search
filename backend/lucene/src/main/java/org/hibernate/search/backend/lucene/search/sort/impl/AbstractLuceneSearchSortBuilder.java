/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;


/**
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneSearchSortBuilder implements SearchSortBuilder<LuceneSearchSortBuilder>,
		LuceneSearchSortBuilder {

	protected SortOrder order;

	@Override
	public LuceneSearchSortBuilder toImplementation() {
		return this;
	}

	@Override
	public void order(SortOrder order) {
		this.order = order;
	}

}
