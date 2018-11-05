/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.mapper.orm.hibernate.FullTextQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.hibernate.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.search.spi.HibernateOrmSearchTarget;
import org.hibernate.search.mapper.pojo.search.PojoReference;

class FullTextSearchTargetImpl<T> implements FullTextSearchTarget<T> {

	private final HibernateOrmSearchTarget<T> delegate;

	FullTextSearchTargetImpl(HibernateOrmSearchTarget<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public FullTextQueryResultDefinitionContext<T> query() {
		return delegate.jpaQuery();
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
	public SearchProjectionFactoryContext<PojoReference, T> projection() {
		return delegate.projection();
	}
}
