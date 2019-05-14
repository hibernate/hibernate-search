/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

/**
 * An extension of {@link ElasticsearchWorkOrchestrator} exposing methods to control its lifecycle.
 */
public interface ElasticsearchWorkOrchestratorImplementor extends AutoCloseable, ElasticsearchWorkOrchestrator {

	@Override
	void close();

	/**
	 * Start any resource necessary to operate the orchestrator at runtime.
	 * <p>
	 * Called by the owner of this orchestrator once after bootstrap,
	 * before any other method is called.
	 */
	void start();

}
