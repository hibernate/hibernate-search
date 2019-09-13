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

	private final SearchSessionWritePlanContext context;

	SearchSessionWritePlanImpl(SearchSessionWritePlanContext context) {
		this.context = context;
	}

	@Override
	public void addOrUpdate(Object entity) {
		context.getCurrentWorkPlan( true ).update( entity );
	}

	@Override
	public void delete(Object entity) {
		context.getCurrentWorkPlan( true ).delete( entity );
	}

	@Override
	public void purge(Class<?> entityClass, Object providedId) {
		context.getCurrentWorkPlan( true ).purge( entityClass, providedId );
	}

	@Override
	public void process() {
		PojoWorkPlan plan = context.getCurrentWorkPlan( false );
		if ( plan == null ) {
			return;
		}
		plan.prepare();
	}

	@Override
	public void execute() {
		PojoWorkPlan plan = context.getCurrentWorkPlan( false );
		if ( plan == null ) {
			return;
		}
		context.getAutomaticIndexingSynchronizationStrategy().handleFuture(
				plan.execute()
		);
	}
}
