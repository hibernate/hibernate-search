/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
