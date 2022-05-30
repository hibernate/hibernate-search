/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

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
	 * Prepare for the mapping of types
	 * and inform the engine of the names of all backends this mapper depends on.
	 * <p>
	 * Called exactly once just before {@link #mapTypes(MappedIndexManagerFactory)}.
	 *
	 * @param backendsInfo A collector of backend names and other info.
	 */
	void prepareMappedTypes(BackendsInfo backendsInfo);

	/**
	 * Begin the creation of a mapping for all mapped types.
	 * <p>
	 * Called exactly once just after {@link #prepareMappedTypes(BackendsInfo)} and before {@link #prepareBuild()}.
	 *
	 * @param indexManagerFactory A factory for index managers,
	 * supporting all the backends declared in {@link #prepareMappedTypes(BackendsInfo)}.
	 */
	void mapTypes(MappedIndexManagerFactory indexManagerFactory);

	/**
	 * Partially build the mapping based on the {@link #mapTypes(MappedIndexManagerFactory) indexed types}
	 * added so far.
	 * <p>
	 * Called exactly once just after {@link #mapTypes(MappedIndexManagerFactory)}.
	 *
	 * @return The partially-built mapping.
	 * @throws MappingAbortedException When aborting the mapping due to
	 * {@link ContextualFailureCollector#add(Throwable) collected} failures.
	 */
	MPBS prepareBuild() throws MappingAbortedException;

}
