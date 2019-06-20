/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.scope.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.loading.spi.ReferenceHitMapper;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.search.loading.context.impl.JavaBeanLoadingContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;

public class SearchScopeImpl implements SearchScope {

	private final ReferenceHitMapper<PojoReference> referenceHitMapper;
	private final PojoScopeDelegate<PojoReference, Void, Void> delegate;

	public SearchScopeImpl(ReferenceHitMapper<PojoReference> referenceHitMapper,
			PojoScopeDelegate<PojoReference, Void, Void> delegate) {
		this.referenceHitMapper = referenceHitMapper;
		this.delegate = delegate;
	}

	@Override
	public SearchQueryResultDefinitionContext<?, PojoReference, ?, ?, ?> search() {
		return delegate.search( new JavaBeanLoadingContext.Builder( referenceHitMapper ) );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortFactoryContext sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactoryContext<PojoReference, ?> projection() {
		return delegate.projection();
	}
}
