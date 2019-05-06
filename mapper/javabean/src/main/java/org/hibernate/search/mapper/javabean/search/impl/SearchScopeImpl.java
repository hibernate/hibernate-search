/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.mapper.javabean.search.SearchScope;
import org.hibernate.search.mapper.javabean.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.javabean.search.dsl.query.impl.SearchQueryResultDefinitionContextImpl;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public class SearchScopeImpl implements SearchScope {

	private final PojoSearchScopeDelegate<?, Void> delegate;

	public SearchScopeImpl(PojoSearchScopeDelegate<?, Void> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryResultDefinitionContext search() {
		return new SearchQueryResultDefinitionContextImpl( delegate );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortContainerContext sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactoryContext<PojoReference, ?> projection() {
		return delegate.projection();
	}
}
