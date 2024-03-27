/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.mapping.spi;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;

public interface BackendMapperContext {
	BackendMappingHints hints();
}
