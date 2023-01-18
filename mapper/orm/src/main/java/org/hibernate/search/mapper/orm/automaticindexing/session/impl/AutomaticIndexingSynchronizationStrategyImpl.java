/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.session.impl;


@SuppressWarnings( "deprecation" )
public final class AutomaticIndexingSynchronizationStrategyImpl {
	private AutomaticIndexingSynchronizationStrategyImpl() {
	}

	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy ASYNC = new DelegatingAutomaticIndexingSynchronizationStrategy(
			HibernateOrmIndexingPlanSynchronizationStrategyImpl.ASYNC );
	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy WRITE_SYNC = new DelegatingAutomaticIndexingSynchronizationStrategy(
			HibernateOrmIndexingPlanSynchronizationStrategyImpl.WRITE_SYNC );
	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy READ_SYNC = new DelegatingAutomaticIndexingSynchronizationStrategy(
			HibernateOrmIndexingPlanSynchronizationStrategyImpl.READ_SYNC );
	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy SYNC = new DelegatingAutomaticIndexingSynchronizationStrategy(
			HibernateOrmIndexingPlanSynchronizationStrategyImpl.SYNC );
}
