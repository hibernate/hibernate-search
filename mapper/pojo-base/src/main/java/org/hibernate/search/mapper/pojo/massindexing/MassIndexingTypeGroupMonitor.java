/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import java.util.OptionalLong;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A component that monitors progress of mass indexing within a type group.
 * <p>
 * Implementations must be threadsafe.
 */
@Incubating
public interface MassIndexingTypeGroupMonitor {

	/**
	 * Notify the monitor that {@code increment} more documents have been added to the index in this type group.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of documents that have been added to the index so far for this type group.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code documentsAdded(3)} and then {@code documentsAdded(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents have been added to the index for this type group".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of documents built
	 */
	void documentsAdded(long increment);

	/**
	 * Notify the monitor that indexing of the type group is starting
	 * and provide the expected number of entities in the group, if known.
	 *
	 * @param totalCount An optional containing the expected number of entities in the group to index, if known,
	 * otherwise an empty optional is supplied.
	 */
	void indexingStarted(OptionalLong totalCount);

	/**
	 * Notify the monitor that indexing of the type group is completed.
	 */
	void indexingCompleted();

}
