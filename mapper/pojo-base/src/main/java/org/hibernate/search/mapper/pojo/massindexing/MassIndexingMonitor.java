/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import org.hibernate.search.mapper.pojo.massindexing.impl.LegacyDelegatingMassIndexingTypeGroupMonitor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A component that monitors progress of mass indexing.
 * <p>
 * As a MassIndexer can take some time to finish its job,
 * it is often necessary to monitor its progress.
 * The default, built-in monitor logs progress periodically at the INFO level.
 * <p>
 * Implementations must be threadsafe.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public interface MassIndexingMonitor {

	/**
	 * Creates a type-group-specific monitor.
	 * <p>
	 * The mass indexer may group some of the types it has to index or index them separately.
	 * The type group represents this combination of types that are retrieved for indexing
	 * in the same pipeline.
	 * <p>
	 * The mass indexer will request to create a type group monitor in the main indexing thread
	 * when initializing the mass indexing environment.
	 *
	 * @param context Describes the type group for which the monitor is requested.
	 * @return A type group mass indexing monitor. By default, a no-op monitor is returned.
	 * @see MassIndexingTypeGroupMonitor
	 */
	@Incubating
	default MassIndexingTypeGroupMonitor typeGroupMonitor(MassIndexingTypeGroupMonitorCreateContext context) {
		return new LegacyDelegatingMassIndexingTypeGroupMonitor( this, context );
	}

	/**
	 * Notify the monitor that {@code increment} more documents have been added to the index.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of documents that have been added to the index so far.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code documentsAdded(3)} and then {@code documentsAdded(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents have been added to the index".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of documents built
	 */
	void documentsAdded(long increment);

	/**
	 * Notify the monitor that {@code increment} more documents have been built.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of documents that have been built so far.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code documentsBuilt(3)} and then {@code documentsBuilt(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents have been built".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of documents built
	 */
	void documentsBuilt(long increment);

	/**
	 * Notify the monitor that {@code increment} more entities have been loaded from the database.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of entities that have been loaded so far.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code entitiesLoaded(3)} and then {@code entitiesLoaded(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents have been loaded".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of entities loaded from database
	 */
	void entitiesLoaded(long increment);

	/**
	 * Notify the monitor that {@code increment} more entities have been
	 * detected in the database and will be indexed.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of entities that Hibernate Search plans to index.
	 * This number can be incremented during indexing
	 * as Hibernate Search moves from one entity type to the next.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code addToTotalCount(3)} and then {@code addToTotalCount(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents will be indexed".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of entities that will be indexed
	 * @deprecated Use {@link MassIndexingTypeGroupMonitor#indexingStarted(MassIndexingTypeGroupMonitorContext)}
	 * and get the total count, if available, from the {@link MassIndexingTypeGroupMonitorContext#totalCount()}.
	 * Alternatively, use the {@link #typeGroupMonitor(MassIndexingTypeGroupMonitorCreateContext)}
	 * and obtain the count from {@link MassIndexingTypeGroupMonitorCreateContext#totalCount()} if
	 * a count is needed before any indexing processes are started.
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	default void addToTotalCount(long increment) {
		// do nothing;
	}

	/**
	 * Notify the monitor that indexing is complete.
	 */
	void indexingCompleted();
}
