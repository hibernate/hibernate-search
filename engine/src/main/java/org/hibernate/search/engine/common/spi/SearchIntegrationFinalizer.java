/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.spi;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizer;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;

public interface SearchIntegrationFinalizer {

	/**
	 * Finalize the building of a mapping.
	 * @param mappingKey The mapping key allowing to retrieve the pre-built mapping.
	 * @param finalizer The object responsible for turning a pre-built mapping into a fully-built mapping
	 * (it may hold some additional context).
	 * @param <PBM> The type of the partially-built mapping.
	 * @param <M> The type of the fully-built mapping.
	 * @return The fully-built mapping.
	 */
	<PBM, M> M finalizeMapping(MappingKey<PBM, M> mappingKey, MappingFinalizer<PBM, M> finalizer);

	/**
	 * Finalize the building of the integration.
	 * @return The fully-built integration.
	 */
	SearchIntegration finalizeIntegration();

}
