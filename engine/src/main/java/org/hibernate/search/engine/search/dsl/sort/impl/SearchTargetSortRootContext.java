/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

public final class SearchTargetSortRootContext<B> extends SearchSortContainerContextImpl<B> {

	public SearchTargetSortRootContext(SearchSortBuilderFactory<?, B> factory) {
		super( factory, new RootSearchSortDslContextImpl<>( factory ) );
	}

}
