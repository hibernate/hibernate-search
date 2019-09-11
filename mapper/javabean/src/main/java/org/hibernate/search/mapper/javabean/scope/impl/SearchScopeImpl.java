/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.scope.impl;

import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.loading.spi.ReferenceHitMapper;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.search.loading.context.impl.JavaBeanLoadingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;

public class SearchScopeImpl implements SearchScope {

	private final ReferenceHitMapper<EntityReference> referenceHitMapper;
	private final PojoScopeDelegate<EntityReference, Void, Void> delegate;

	public SearchScopeImpl(ReferenceHitMapper<EntityReference> referenceHitMapper,
			PojoScopeDelegate<EntityReference, Void, Void> delegate) {
		this.referenceHitMapper = referenceHitMapper;
		this.delegate = delegate;
	}

	@Override
	public SearchQueryHitTypeStep<?, EntityReference, ?, ?, ?> search() {
		return delegate.search( new JavaBeanLoadingContext.Builder( referenceHitMapper ) );
	}

	@Override
	public SearchPredicateFactory predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortFactory sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactory<EntityReference, ?> projection() {
		return delegate.projection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}
}
