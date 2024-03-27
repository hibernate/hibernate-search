/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface SearchIndexingPlanExecutionReport {

	/**
	 * @return The {@link Exception} or {@link Error} thrown when indexing failed,
	 * or {@link Optional#empty()} if indexing succeeded.
	 */
	Optional<Throwable> throwable();

	/**
	 * @return A list of references to entities that may not be indexed correctly as a result of the failure.
	 * Never {@code null}, but may be empty.
	 */
	List<? extends EntityReference> failingEntities();

}
