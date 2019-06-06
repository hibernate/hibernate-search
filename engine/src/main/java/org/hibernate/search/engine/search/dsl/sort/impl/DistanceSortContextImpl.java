/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.dsl.sort.DistanceSortContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.dsl.sort.spi.AbstractNonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceSortContextImpl<B>
		extends AbstractNonEmptySortContext<B>
		implements DistanceSortContext {

	private final DistanceSortBuilder<B> builder;

	DistanceSortContextImpl(SearchSortDslContext<?, B> dslContext,
			String absoluteFieldPath, GeoPoint location) {
		super( dslContext );
		this.builder = dslContext.getFactory().distance( absoluteFieldPath, location );
	}

	@Override
	public DistanceSortContext order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}

}
