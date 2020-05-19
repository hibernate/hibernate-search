/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

public class ElasticsearchSimpleWorkOrchestrator
		extends AbstractElasticsearchWorkOrchestrator<ElasticsearchSimpleWorkOrchestrator.WorkExecution<?>>
		implements ElasticsearchParallelWorkOrchestrator {

	private ElasticsearchWorkExecutionContext executionContext;

	public ElasticsearchSimpleWorkOrchestrator(String name, ElasticsearchLink link) {
		super( name, link );
	}

	@Override
	public <T> CompletableFuture<T> submit(NonBulkableWork<T> work) {
		WorkExecution<T> workExecution = new WorkExecution<>( work );
		submit( workExecution );
		return workExecution.getResult();
	}

	@Override
	protected void doStart(ConfigurationPropertySource propertySource) {
		executionContext = createWorkExecutionContext();
	}

	@Override
	protected void doSubmit(WorkExecution<?> work) {
		work.execute( executionContext );
	}

	@Override
	protected CompletableFuture<?> completion() {
		// We do not wait for these works to finish;
		// callers were provided with a future and are responsible for waiting
		// before they close the application.
		return CompletableFuture.completedFuture( null );
	}

	@Override
	protected void doStop() {
		executionContext = null;
	}

	static class WorkExecution<T> {
		private final NonBulkableWork<T> work;

		private CompletableFuture<T> result;

		WorkExecution(NonBulkableWork<T> work) {
			this.work = work;
		}

		public void execute(ElasticsearchWorkExecutionContext executionContext) {
			result = work.execute( executionContext );
		}

		public CompletableFuture<T> getResult() {
			return result;
		}
	}

}
