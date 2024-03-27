/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

public interface PojoMassIndexingSessionContext extends PojoWorkSessionContext {

	PojoIndexer createIndexer();

	@Override
	PojoRuntimeIntrospector runtimeIntrospector();

	@Override
	String tenantIdentifier();
}
