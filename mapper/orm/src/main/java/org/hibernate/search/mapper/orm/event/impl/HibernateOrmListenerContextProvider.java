/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.automaticindexing.session.impl.ConfiguredAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public interface HibernateOrmListenerContextProvider {

	HibernateOrmListenerTypeContextProvider getTypeContextProvider();

	PojoIndexingPlan getCurrentIndexingPlan(SessionImplementor session, boolean createIfDoesNotExist);

	ConfiguredAutomaticIndexingSynchronizationStrategy getCurrentAutomaticIndexingSynchronizationStrategy(
			SessionImplementor session);

}
