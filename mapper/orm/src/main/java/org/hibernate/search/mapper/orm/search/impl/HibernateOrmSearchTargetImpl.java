/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchTarget;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public class HibernateOrmSearchTargetImpl<T> implements HibernateOrmSearchTarget<T> {

	private final PojoSearchTargetDelegate<T> searchTargetDelegate;
	private final SessionImplementor sessionImplementor;

	public HibernateOrmSearchTargetImpl(PojoSearchTargetDelegate<T> searchTargetDelegate,
			SessionImplementor sessionImplementor) {
		this.searchTargetDelegate = searchTargetDelegate;
		this.sessionImplementor = sessionImplementor;
	}

	@Override
	public HibernateOrmSearchQueryResultDefinitionContext<T> jpaQuery() {
		return new HibernateOrmSearchQueryResultDefinitionContextImpl<>( searchTargetDelegate, sessionImplementor );
	}

	@Override
	public SearchQueryResultDefinitionContext<PojoReference, T> query() {
		ObjectLoaderBuilder<T> objectLoaderBuilder = new ObjectLoaderBuilder<>(
				sessionImplementor,
				searchTargetDelegate.getTargetedIndexedTypes()
		);
		MutableObjectLoadingOptions mutableObjectLoadingOptions = new MutableObjectLoadingOptions();
		return searchTargetDelegate.query( objectLoaderBuilder.build( mutableObjectLoadingOptions ) );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return searchTargetDelegate.predicate();
	}

	@Override
	public SearchSortContainerContext sort() {
		return searchTargetDelegate.sort();
	}

	@Override
	public SearchProjectionFactoryContext projection() {
		return searchTargetDelegate.projection();
	}
}
