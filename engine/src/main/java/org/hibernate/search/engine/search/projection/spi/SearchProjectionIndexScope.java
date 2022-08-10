/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
