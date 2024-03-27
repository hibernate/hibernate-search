/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.schema.management.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;

public interface SchemaManagementIndexManagerContext {

	Collection<LuceneParallelWorkOrchestrator> allManagementOrchestrators();

	Optional<String> backendName();
}
