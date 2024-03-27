/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;

/**
 * A builder for {@link org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager} instances,
 * which will be the interface between the mapping and the index when indexing and searching.
 * <p>
 * Exposes in particular the {@link IndexedEntityBindingContext binding context},
 * allowing the mapper to declare index fields that will be bound to entity properties.
 */
public interface MappedIndexManagerBuilder {

	IndexedEntityBindingContext rootBindingContext();

	MappedIndexManager build();

}
