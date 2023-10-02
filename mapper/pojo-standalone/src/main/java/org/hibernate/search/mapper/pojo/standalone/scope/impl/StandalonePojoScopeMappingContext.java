/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.scope.impl;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;

public interface StandalonePojoScopeMappingContext extends BackendMappingContext {

	StandalonePojoLoadingContext.Builder loadingContextBuilder();

}
