/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.mapper.pojo.identity.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

/**
 * A collector of extended mapping information.
 * <p>
 * This should be implemented by POJO mapper implementors in order to collect metadata
 * necessary to implement their {@link org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider}.
 */
public interface PojoTypeExtendedMappingCollector {

	default void documentIdSourceProperty(PojoPropertyModel<?> documentIdSourceProperty) {
		// Default implementation: ignore this information.
	}

	default void identifierMapping(IdentifierMapping identifierMapping) {
		// Default implementation: ignore this information.
	}

	default void dirtyFilter(PojoPathFilter dirtyFilter) {
		// Default implementation: ignore this information.
	}

	default void dirtyContainingAssociationFilter(PojoPathFilter filter) {
		// Default implementation: ignore this information.
	}

}
