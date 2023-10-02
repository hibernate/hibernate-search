/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;

/**
 * An interface with knowledge of the index manager internals,
 * able to retrieve components related to work execution.
 * <p>
 * Note this interface exists mainly to more cleanly pass information
 * from the index manager to the various work execution components.
 * If we just passed the index manager to the various work execution components,
 * we would have a cyclic dependency.
 * If we passed all the components held by the index manager to the various work execution components,
 * we would end up with methods with many parameters.
 */
public interface WorkExecutionIndexManagerContext {

	String mappedTypeName();

	LuceneSerialWorkOrchestrator indexingOrchestrator(String documentId, String routingKey);

	List<LuceneParallelWorkOrchestrator> managementOrchestrators(Set<String> routingKeys);

	List<LuceneParallelWorkOrchestrator> allManagementOrchestrators();
}
