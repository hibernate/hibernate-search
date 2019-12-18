/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

/**
 * A handler for failures occurring during mass indexing.
 * <p>
 * The handler should be used to report failures to application maintainers.
 * The default failure handler simply delegates to the configured {@link org.hibernate.search.engine.reporting.FailureHandler},
 * which by default logs failures at the {@code ERROR} level,
 * but it can be replaced with a custom implementations
 * by configuring the mass indexer.
 * <p>
 * Handlers can be called from multiple threads simultaneously: implementations must be thread-safe.
 */
public interface MassIndexingFailureHandler {

	/**
	 * Handle a generic failure.
	 * <p>
	 * This method is expected to report the failure somewhere (logs, ...),
	 * then return as quickly as possible.
	 * Heavy error processing (sending emails, ...), if any, should be done asynchronously.
	 * <p>
	 * Any error or exception thrown by this method will be caught by Hibernate Search and logged.
	 *
	 * @param context Contextual information about the failure (throwable, operation, ...)
	 */
	void handle(MassIndexingFailureContext context);

	/**
	 * Handle a failure when indexing an entity.
	 * <p>
	 * This method is expected to report the failure somewhere (logs, ...),
	 * then return as quickly as possible.
	 * Heavy error processing (sending emails, ...), if any, should be done asynchronously.
	 * <p>
	 * Any error or exception thrown by this method will be caught by Hibernate Search and logged.
	 *
	 * @param context Contextual information about the failure (throwable, operation, ...)
	 */
	default void handle(MassIndexingEntityFailureContext context) {
		handle( (MassIndexingFailureContext) context );
	}

}
