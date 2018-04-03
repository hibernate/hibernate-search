/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

public interface Mapper<M> {

	/**
	 * @param typeModel A model of the type to be mapped
	 * @param indexManagerBuildingState The building state for the index to be mapped
	 */
	void addIndexed(MappableTypeModel typeModel, IndexManagerBuildingState<?> indexManagerBuildingState);

	MappingImplementor<M> build();

	boolean isMultiTenancyEnabled();

}
