/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
