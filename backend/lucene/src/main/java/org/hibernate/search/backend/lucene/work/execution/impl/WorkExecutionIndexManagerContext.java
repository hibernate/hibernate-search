/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	String getIndexName();

	String getMappedTypeName();

	LuceneSerialWorkOrchestrator getIndexingOrchestrator(String documentId, String routingKey);

	List<LuceneParallelWorkOrchestrator> getManagementOrchestrators(Set<String> routingKeys);

	List<LuceneParallelWorkOrchestrator> getAllManagementOrchestrators();
}
