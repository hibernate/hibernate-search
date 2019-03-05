/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.mapper.orm.search.dsl.query.FullTextQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.search.dsl.query.impl.FullTextQueryResultDefinitionContextImpl;
import org.hibernate.search.mapper.orm.search.spi.HibernateOrmSearchTarget;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchTargetDelegate;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public class HibernateOrmSearchTargetImpl<O> implements HibernateOrmSearchTarget<O> {

	private final PojoSearchTargetDelegate<O, O> searchTargetDelegate;
	private final SessionImplementor sessionImplementor;

	public HibernateOrmSearchTargetImpl(PojoSearchTargetDelegate<O, O> searchTargetDelegate,
			SessionImplementor sessionImplementor) {
		this.searchTargetDelegate = searchTargetDelegate;
		this.sessionImplementor = sessionImplementor;
	}

	@Override
	public FullTextQueryResultDefinitionContext<O> jpaQuery() {
		return new FullTextQueryResultDefinitionContextImpl<>( searchTargetDelegate, sessionImplementor );
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
	public SearchProjectionFactoryContext<PojoReference, O> projection() {
		return searchTargetDelegate.projection();
	}
}
