/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.impl.BuildingRootSearchSortDslContextImpl;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchSortContainerContextImpl;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

public final class SearchTargetSortRootContext<C> extends SearchSortContainerContextImpl<SearchSort, C> {

	public SearchTargetSortRootContext(SearchSortFactory<C> factory) {
		super( factory, new BuildingRootSearchSortDslContextImpl<>( factory ) );
	}

}
