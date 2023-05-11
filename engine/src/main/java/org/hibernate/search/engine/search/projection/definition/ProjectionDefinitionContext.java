/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.definition;

import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link ProjectionDefinition#create(SearchProjectionFactory, ProjectionDefinitionContext)}.
 * @see ProjectionDefinition#create(SearchProjectionFactory, ProjectionDefinitionContext)
 */
@Incubating
public interface ProjectionDefinitionContext {

	// TODO HSEARCH-4806/HSEARCH-4807 expose parameters here, to be defined in the query, useful in particular for the distance projection.

}
