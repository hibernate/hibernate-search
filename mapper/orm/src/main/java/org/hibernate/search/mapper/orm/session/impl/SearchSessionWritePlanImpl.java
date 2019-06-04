/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;

final class SearchSessionWritePlanImpl implements SearchSessionWritePlan {

	private final HibernateSearchContextService contextService;
	private final HibernateOrmSearchSession searchSession;
	private final SessionImplementor ormSession;

	SearchSessionWritePlanImpl(HibernateSearchContextService contextService,
			HibernateOrmSearchSession searchSession,
			SessionImplementor ormSession) {
		this.contextService = contextService;
		this.searchSession = searchSession;
		this.ormSession = ormSession;
	}

	@Override
	public void addOrUpdate(Object entity) {
		getCurrentWorkPlan().update( entity );
	}

	@Override
	public void delete(Object entity) {
		getCurrentWorkPlan().delete( entity );
	}

	@Override
	public void purge(Class<?> entityClass, Object providedId) {
		getCurrentWorkPlan().purge( entityClass, providedId );
	}

	@Override
	public void process() {
		getCurrentWorkPlan().prepare();
	}

	@Override
	public void execute() {
		searchSession.getAutomaticIndexingSynchronizationStrategy().handleFuture(
				getCurrentWorkPlan().execute()
		);
	}

	private PojoWorkPlan getCurrentWorkPlan() {
		searchSession.checkOrmSessionIsOpen();
		return contextService.getCurrentWorkPlan( ormSession );
	}
}
