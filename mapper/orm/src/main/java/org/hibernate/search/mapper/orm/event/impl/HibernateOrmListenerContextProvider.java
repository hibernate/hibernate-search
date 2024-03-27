/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.event.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoTypeIndexingPlan;

public interface HibernateOrmListenerContextProvider {

	HibernateOrmListenerTypeContextProvider typeContextProvider();

	boolean listenerEnabled();

	PojoIndexingPlan currentIndexingPlanIfExisting(SessionImplementor session);

	PojoTypeIndexingPlan currentIndexingPlanIfTypeIncluded(SessionImplementor session, PojoRawTypeIdentifier<?> typeIdentifier);

	ConfiguredIndexingPlanSynchronizationStrategy currentAutomaticIndexingSynchronizationStrategy(
			SessionImplementor session);

}
