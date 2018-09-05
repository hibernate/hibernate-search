/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;

/**
 * An {@link ElasticsearchWorkOrchestrator} requiring a call to {@link #flush()}
 * to finalize orchestration, and calls to {@link #reset()} before re-use.
 *
 * @author Yoann Rodiere
 */
interface FlushableElasticsearchWorkOrchestrator extends ElasticsearchWorkOrchestrator {

	/**
	 * Ensure all works submitted since the last call to {@link #reset()} will
	 * actually be executed.
	 *
	 * @return A future completing when all submitted since the last call to {@link #reset()}
	 * have completed.
	 */
	CompletableFuture<Void> flush();

	void reset();

}
