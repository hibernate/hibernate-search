/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateFactoryContextImpl;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.SearchProjectionFactoryContextImpl;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchTargetSortRootContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;

public abstract class IndexSearchTargetBase implements IndexSearchTarget {

	@Override
	public SearchPredicateFactoryContext predicate() {
		return new SearchPredicateFactoryContextImpl<>( getSearchTargetContext().getSearchPredicateBuilderFactory() );
	}

	@Override
	public SearchSortContainerContext sort() {
		return new SearchTargetSortRootContext<>( getSearchTargetContext().getSearchSortBuilderFactory() );
	}

	@Override
	public SearchProjectionFactoryContext projection() {
		return new SearchProjectionFactoryContextImpl( getSearchTargetContext().getSearchProjectionFactory() );
	}

	protected abstract SearchTargetContext<?> getSearchTargetContext();

}
