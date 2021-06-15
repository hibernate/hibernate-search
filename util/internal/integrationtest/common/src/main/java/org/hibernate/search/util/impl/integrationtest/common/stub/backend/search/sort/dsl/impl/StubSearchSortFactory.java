/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;

public class StubSearchSortFactory
		extends AbstractSearchSortFactory<StubSearchSortFactory, SearchSortIndexScope<?>, SearchPredicateFactory> {
	public StubSearchSortFactory(SearchSortDslContext<SearchSortIndexScope<?>, SearchPredicateFactory> dslContext) {
		super( dslContext );
	}
}
