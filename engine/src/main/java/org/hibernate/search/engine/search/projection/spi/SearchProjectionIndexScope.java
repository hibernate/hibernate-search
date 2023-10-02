/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;

public interface SearchProjectionIndexScope<S extends SearchProjectionIndexScope<?>>
		extends SearchIndexScope<S> {

	SearchProjectionBuilderFactory projectionBuilders();

	ProjectionRegistry projectionRegistry();

	List<? extends ProjectionMappedTypeContext> mappedTypeContexts();

}
