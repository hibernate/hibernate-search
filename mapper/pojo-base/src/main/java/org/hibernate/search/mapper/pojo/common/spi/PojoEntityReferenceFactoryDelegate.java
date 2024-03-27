/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.common.spi;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * A delegate for the POJO implementation of {@link org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory}.
 * <p>
 * Implementations of this class generally simply call a constructor.
 */
@FunctionalInterface
public interface PojoEntityReferenceFactoryDelegate {

	EntityReference create(PojoRawTypeIdentifier<?> typeIdentifier, String name, Object id);

}
