/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.dsl.spi.AbstractSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionIndexScope;

public class StubSearchProjectionFactory<R, E>
		extends AbstractSearchProjectionFactory<StubSearchProjectionFactory<R, E>, SearchProjectionIndexScope<?>, R, E> {
	public StubSearchProjectionFactory(SearchProjectionDslContext<SearchProjectionIndexScope<?>> dslContext) {
		super( dslContext );
	}

	@Override
	public StubSearchProjectionFactory<R, E> withRoot(String objectFieldPath) {
		return new StubSearchProjectionFactory<>( dslContext.rescope( dslContext.scope().withRoot( objectFieldPath ) ) );
	}
}
