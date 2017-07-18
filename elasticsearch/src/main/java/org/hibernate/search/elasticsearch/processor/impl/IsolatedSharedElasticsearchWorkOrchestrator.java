/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;


/**
 * An orchestrator shared across multiple threads,
 * but not sharing any context between calls, even from the same thread.
 * <p>
 * Orchestration is performed at the changeset level,
 * preventing bulks to span multiple changesets.
 *
 * @author Yoann Rodiere
 */
class IsolatedSharedElasticsearchWorkOrchestrator implements ElasticsearchWorkOrchestrator {

	private final Supplier<FlushableElasticsearchWorkOrchestrator> delegateSupplier;

	/**
	 * @param delegateSupplier A <strong>thread-safe</strong> supplier returning a new orchestrator for each call to {@code get()}.
	 */
	public IsolatedSharedElasticsearchWorkOrchestrator(Supplier<FlushableElasticsearchWorkOrchestrator> delegateSupplier) {
		this.delegateSupplier = delegateSupplier;
	}

	@Override
	public CompletableFuture<Void> submit(Iterable<ElasticsearchWork<?>> works) {
		FlushableElasticsearchWorkOrchestrator delegate = delegateSupplier.get();
		delegate.submit( works );
		return delegate.flush();
	}

}
