/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.util.common.reporting.EventContextElement;

/**
 * Tagging interface for objects used as a key to retrieve mappings at different states:
 * when finalizing the mapping in {@link SearchIntegrationFinalizer#finalizeMapping(MappingKey, MappingFinalizer)}.
 *
 * @param <PBM> The type of pre-built mappings (see {@link SearchIntegrationFinalizer#finalizeMapping(MappingKey, MappingFinalizer)}
 * @param <M> The type of fully-built mappings
 */
public interface MappingKey<PBM, M> extends EventContextElement {

}
