/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.mapping.spi;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;

/**
 * Provides visibility from the lower layers of Hibernate Search (engine, backend)
 * to the mapping defined in the upper layers.
 */
public interface BackendMappingContext {

	BackendMappingHints hints();

	ToDocumentValueConvertContext toDocumentValueConvertContext();

	ProjectionRegistry projectionRegistry();

	ProjectionMappedTypeContext mappedTypeContext(String mappedTypeName);

	/**
	 * @return A {@link EntityReferenceFactory}.
	 */
	EntityReferenceFactory entityReferenceFactory();

}
