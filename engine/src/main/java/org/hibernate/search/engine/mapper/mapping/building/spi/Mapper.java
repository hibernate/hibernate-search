/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * @param <MPBS> The Java type of the partial build state of the produced mapping.
 */
public interface Mapper<MPBS extends MappingPartialBuildState> {

	/**
	 * Close any allocated resource.
	 * <p>
	 * This method is called when an error occurs while starting up Hibernate Search.
	 * When this method is called, it is guaranteed to be the last call on the mapper.
	 */
	void closeOnFailure();

	/**
	 * Add an indexed type to the mapping that will be {@link #prepareBuild() built}.
	 * <p>
	 * Never called after {@link #prepareBuild()}.
	 *
	 * @param typeModel A model of the type to be mapped
	 * @param indexManagerBuildingState The building state for the index to be mapped
	 */
	void addIndexed(MappableTypeModel typeModel, IndexManagerBuildingState<?> indexManagerBuildingState);

	/**
	 * Partially build the mapping based on the {@link #addIndexed(MappableTypeModel, IndexManagerBuildingState) indexed types}
	 * added so far.
	 * <p>
	 * May only be called once on a given object.
	 * <p>
	 * The
	 * </p>
	 *
	 * @return The partially-built mapping.
	 * @throws MappingAbortedException When aborting the mapping due to
	 * {@link ContextualFailureCollector#add(Throwable) collected} failures.
	 */
	MPBS prepareBuild() throws MappingAbortedException;

}
