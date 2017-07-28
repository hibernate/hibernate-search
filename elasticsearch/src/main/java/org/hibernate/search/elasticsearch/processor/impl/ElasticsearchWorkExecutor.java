/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;

/**
 * @author Yoann Rodiere
 */
interface ElasticsearchWorkExecutor {

	/**
	 * @param work A work to execute
	 * @param executionContext The execution context
	 * @return A {@link CompletableFuture} that will hold the result of the execution.
	 */
	<T> CompletableFuture<T> submit(ElasticsearchWork<T> work, ElasticsearchWorkExecutionContext executionContext);

}
