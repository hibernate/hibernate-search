/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.spi;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

/**
 * Session-scoped information and operations for use in POJO indexing processors.
 */
public interface PojoIndexingProcessorSessionContext extends BridgeSessionContext {

	PojoRuntimeIntrospector runtimeIntrospector();

}
