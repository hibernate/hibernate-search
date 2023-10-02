/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.impl;

import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;

public final class DefaultProjectionDefinitionContext
		implements ProjectionDefinitionContext {
	// TODO HSEARCH-4806/HSEARCH-4807 have the query create an instance when instantiating projections
	public static final DefaultProjectionDefinitionContext INSTANCE =
			new DefaultProjectionDefinitionContext();

	private DefaultProjectionDefinitionContext() {
	}
}
