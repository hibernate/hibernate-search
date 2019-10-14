/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

public interface AutomaticIndexingSynchronizationConfigurationContext {

	/**
	 * @param strategy A strategy describing how commits should be handled after document changes are applied.
	 * Defaults to {@link DocumentCommitStrategy#NONE}.
	 */
	void documentCommitStrategy(DocumentCommitStrategy strategy);

	/**
	 * @param strategy A strategy describing how refresh should be handled after document changes are applied.
	 * Defaults to {@link DocumentRefreshStrategy#NONE}.
	 */
	void documentRefreshStrategy(DocumentRefreshStrategy strategy);

	/**
	 * Set the handler for the (asynchronous) indexing future.
	 * <p>
	 * This typically involves waiting on the given future,
	 * to prevent the thread from resuming execution until indexing is complete.
	 *
	 * @param handler A handler that will be passed a future representing the progress of indexing.
	 * Defaults to a no-op handler.
	 * The future will be completed once all document changes are applied
	 * and the commit/refresh requirements defined by {@link #documentCommitStrategy(DocumentCommitStrategy)}
	 * and {@link #documentRefreshStrategy(DocumentRefreshStrategy)} are satisfied.
	 */
	void indexingFutureHandler(Consumer<CompletableFuture<?>> handler);

}
