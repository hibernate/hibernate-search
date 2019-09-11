/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceSortOptionsStepImpl<B>
		extends AbstractSortThenStep<B>
		implements DistanceSortOptionsStep {

	private final DistanceSortBuilder<B> builder;

	DistanceSortOptionsStepImpl(SearchSortDslContext<?, B> dslContext,
			String absoluteFieldPath, GeoPoint location) {
		super( dslContext );
		this.builder = dslContext.getBuilderFactory().distance( absoluteFieldPath, location );
	}

	@Override
	public DistanceSortOptionsStep order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}

}
