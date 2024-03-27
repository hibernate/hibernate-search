/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

public interface PojoScopeSessionContext extends BridgeSessionContext, BackendSessionContext {

	@Override
	PojoScopeMappingContext mappingContext();

}
