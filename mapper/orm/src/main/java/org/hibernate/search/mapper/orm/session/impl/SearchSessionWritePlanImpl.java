/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;

final class SearchSessionWritePlanImpl implements SearchSessionWritePlan {

	private final HibernateOrmSearchSession searchSession;

	SearchSessionWritePlanImpl(HibernateOrmSearchSession searchSession) {
		this.searchSession = searchSession;
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
		return searchSession.getCurrentWorkPlan();
	}
}
