/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public final class SearchIndexingPlanImpl implements SearchIndexingPlan {

	private final SearchIndexingPlanContext context;

	public SearchIndexingPlanImpl(SearchIndexingPlanContext context) {
		this.context = context;
	}

	@Override
	public void addOrUpdate(Object entity) {
		context.getCurrentIndexingPlan( true ).addOrUpdate( null, entity );
	}

	@Override
	public void delete(Object entity) {
		context.getCurrentIndexingPlan( true ).delete( null, entity );
	}

	@Override
	public void purge(Class<?> entityClass, Object providedId) {
		context.getCurrentIndexingPlan( true ).purge( entityClass, providedId );
	}

	@Override
	public void process() {
		PojoIndexingPlan plan = context.getCurrentIndexingPlan( false );
		if ( plan == null ) {
			return;
		}
		plan.process();
	}

	@Override
	public void execute() {
		PojoIndexingPlan plan = context.getCurrentIndexingPlan( false );
		if ( plan == null ) {
			return;
		}
		context.getConfiguredAutomaticIndexingSynchronizationStrategy()
				.executeAndSynchronize( plan );
	}
}
