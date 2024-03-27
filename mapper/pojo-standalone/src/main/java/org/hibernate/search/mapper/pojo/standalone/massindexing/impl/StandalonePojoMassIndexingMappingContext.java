/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.massindexing.impl;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public interface StandalonePojoMassIndexingMappingContext extends PojoMassIndexingMappingContext {

	StandalonePojoMassIndexingSessionContext createSession(String tenantIdentifier);

	PojoRuntimeIntrospector runtimeIntrospector();

}
