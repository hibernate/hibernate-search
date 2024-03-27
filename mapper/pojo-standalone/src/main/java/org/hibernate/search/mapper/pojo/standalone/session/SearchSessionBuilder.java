/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.session;

import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface SearchSessionBuilder {

	/**
	 * @param tenantId The tenant ID to use when performing index-related operations (indexing, searching, ...)
	 * in the resulting session.
	 * @return {@code this} for method chaining.
	 * @deprecated Use {@link #tenantId(Object)} instead.
	 */
	@Deprecated(forRemoval = true)
	SearchSessionBuilder tenantId(String tenantId);

	/**
	 * @param tenantId The tenant ID to use when performing index-related operations (indexing, searching, ...)
	 * in the resulting session.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder tenantId(Object tenantId);

	/**
	 * @param synchronizationStrategy The synchronization strategy for indexing works added to the {@link SearchSession#indexingPlan() indexing plan}.
	 * @return {@code this} for method chaining.
	 *
	 * @see IndexingPlanSynchronizationStrategy
	 * @see IndexingPlanSynchronizationStrategy
	 */
	SearchSessionBuilder indexingPlanSynchronizationStrategy(IndexingPlanSynchronizationStrategy synchronizationStrategy);

	/**
	 * @param loadingOptionsContributor The default loading options.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder loading(Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor);

	/**
	 * @return The resulting session.
	 */
	SearchSession build();

}
