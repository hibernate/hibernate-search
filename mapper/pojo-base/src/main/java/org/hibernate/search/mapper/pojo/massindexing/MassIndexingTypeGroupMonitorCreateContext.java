/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import java.util.OptionalLong;
import java.util.Set;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface MassIndexingTypeGroupMonitorCreateContext {

	/**
	 * Describes the entity types included in the type group.
	 *
	 * @return The set of entity types included in the type group.
	 */
	Set<MassIndexingType> includedTypes();

	/**
	 * Provides a total count of entities within the type group that should be indexed if obtaining such a count is possible.
	 * <p>
	 * <b>Warning</b>: This operation is not cached and a count from the underlying loading strategy
	 * will be requested on each call to get the total count.
	 * <p>
	 * The loaders used to calculate the count provided by this context are <b>not</b> reused by the
	 * indexing process, which means that, in general, the number returned by this context
	 * <b>may not</b> match the number of entities to index once the actual indexing starts.
	 * This can happen when new entities are added/existing ones are removed before the indexing process starts.
	 *
	 * @return The total count of entities to be indexed within the current type group, or an empty optional
	 * if the count cannot be determined by the underlying loading strategy, e.g. when the strategy is based on a stream data
	 * and obtaining count is not possible until all elements of the stream are consumed.
	 */
	OptionalLong totalCount();
}
