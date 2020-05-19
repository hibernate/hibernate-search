/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import org.hibernate.search.mapper.orm.automaticindexing.session.impl.ConfiguredAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public interface SearchIndexingPlanSessionContext {

	ConfiguredAutomaticIndexingSynchronizationStrategy configuredAutomaticIndexingSynchronizationStrategy();

	PojoRuntimeIntrospector runtimeIntrospector();

	PojoIndexingPlan<EntityReference> currentIndexingPlan(boolean createIfDoesNotExist);

}
