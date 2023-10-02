/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.session.spi;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;

/**
 * Provides visibility from the lower layers of Hibernate Search (engine, backend)
 * to the session defined in the upper layers (mapping).
 */
public interface BackendSessionContext {

	BackendMappingContext mappingContext();

	String tenantIdentifier();

}
