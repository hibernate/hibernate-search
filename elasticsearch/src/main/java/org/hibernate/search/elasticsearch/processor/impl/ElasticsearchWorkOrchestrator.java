/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;

/**
 * Aggregates works from changesets, orchestrates their execution and runs them asynchronously.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchWorkOrchestrator {

	CompletableFuture<Void> submit(Iterable<ElasticsearchWork<?>> nonBulkedWorks);

	default CompletableFuture<?> submit(ElasticsearchWork<?> work) {
		return submit( Collections.singleton( work ) );
	}

}
