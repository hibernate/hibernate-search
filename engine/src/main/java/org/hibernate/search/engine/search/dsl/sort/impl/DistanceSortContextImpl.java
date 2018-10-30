/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.dsl.sort.DistanceSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.dsl.sort.spi.NonEmptySortContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceSortContextImpl<B>
		extends NonEmptySortContextImpl
		implements DistanceSortContext, SearchSortContributor<B> {

	private final DistanceSortBuilder<B> builder;

	DistanceSortContextImpl(SearchSortContainerContext containerContext,
			SearchSortFactory<?, B> factory, SearchSortDslContext<?> dslContext,
			String absoluteFieldPath, GeoPoint location) {
		super( containerContext, dslContext );
		this.builder = factory.distance( absoluteFieldPath, location );
	}

	@Override
	public DistanceSortContext order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public void contribute(Consumer<? super B> collector) {
		collector.accept( builder.toImplementation() );
	}
}
