/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.mapping.spi;

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

}
