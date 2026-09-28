package org.hibernate.search.mapper.orm.work.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public interface SearchIndexingPlanSessionContext {

	ConfiguredIndexingPlanSynchronizationStrategy configuredIndexingPlanSynchronizationStrategy();

	PojoRuntimeIntrospector runtimeIntrospector();

	PojoIndexingPlan currentIndexingPlan(boolean createIfDoesNotExist);

	void checkOpen();

}
