/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public interface PojoLoadingSessionContext {

	PojoRuntimeIntrospector runtimeIntrospector();

}
