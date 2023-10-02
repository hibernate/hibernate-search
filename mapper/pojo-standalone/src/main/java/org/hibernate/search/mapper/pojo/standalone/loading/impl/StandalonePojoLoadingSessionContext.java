/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public interface StandalonePojoLoadingSessionContext extends AutoCloseable {

	@Override
	void close();

	String tenantIdentifier();

	PojoRuntimeIntrospector runtimeIntrospector();

}
