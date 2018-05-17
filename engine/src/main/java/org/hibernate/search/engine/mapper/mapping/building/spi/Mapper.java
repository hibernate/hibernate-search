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
	 * Close any allocated resource.
	 * <p>
	 * This method is called when an error occurs while starting up Hibernate Search.
	 * When this method is called, it is guaranteed to be the last call on the mapper.
	 */
	void closeOnFailure();

	/**
	 * Add an indexed type to the mapping that will be {@link #build() built}.
	 * <p>
	 * Never called after {@link #build()}.
	 *
	 * @param typeModel A model of the type to be mapped
	 * @param indexManagerBuildingState The building state for the index to be mapped
	 */
	void addIndexed(MappableTypeModel typeModel, IndexManagerBuildingState<?> indexManagerBuildingState);

	/**
	 * Build the mapping based on the {@link #addIndexed(MappableTypeModel, IndexManagerBuildingState) indexed types}
	 * added so far.
	 * <p>
	 * May only be called once on a given object.
	 *
	 * @return The mapping.
	 */
	MappingImplementor<M> build();

	boolean isMultiTenancyEnabled();

}
