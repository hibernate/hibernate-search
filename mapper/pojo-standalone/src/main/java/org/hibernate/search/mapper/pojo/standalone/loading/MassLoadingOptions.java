/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import java.util.List;

import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface MassLoadingOptions {

	/**
	 * @return How many entities to load and index in each batch.
	 * Defines the maximum expected size of each list of IDs
	 * loaded by {@link MassIdentifierLoader#loadNext()}
	 * and passed to {@link MassEntityLoader#load(List)}.
	 */
	int batchSize();

	/**
	 * Gets context previously passed to
	 * {@link MassIndexer#context(Class, Object)}.
	 *
	 * @param <T> The context type.
	 * @param contextType The context type.
	 * @return The context, i.e. an instance of the given type, or {@code null} if no context was set for this type.
	 */
	<T> T context(Class<T> contextType);

}
