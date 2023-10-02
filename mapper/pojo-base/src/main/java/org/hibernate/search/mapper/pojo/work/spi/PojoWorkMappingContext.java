/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.spi;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;

/**
 * Mapping-scoped information and operations for use in POJO work execution.
 */
public interface PojoWorkMappingContext extends BackendMappingContext, BridgeMappingContext {

	/**
	 * @return A {@link PojoEntityReferenceFactoryDelegate}.
	 */
	PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate();

}
