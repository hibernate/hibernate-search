/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A handler for failures occurring during background operations,
 * which may not have been reported to the caller due to being executed asynchronously.
 * <p>
 * The handler should be used to report failures to application maintainers.
 * The default failure handler simply logs failures at the {@code ERROR} level,
 * but it can be replaced with a custom implementations through
 * {@link org.hibernate.search.engine.cfg.EngineSettings#BACKGROUND_FAILURE_HANDLER a configuration property}.
 * <p>
 * Handlers can be called from multiple threads simultaneously: implementations must be thread-safe.
 *
 * @author Amin Mohammed-Coleman
 */
public interface FailureHandler {

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
	void handle(FailureContext context);

	/**
	 * Handle the failure of entity indexing.
	 * <p>
	 * This method is expected to report the failure somewhere (logs, ...),
	 * then return as quickly as possible.
	 * Heavy error processing (sending emails, ...), if any, should be done asynchronously.
	 * <p>
	 * Any error or exception thrown by this method will be caught by Hibernate Search and logged.
	 *
	 * @param context Contextual information about the failure (throwable, operation, ...)
	 */
	void handle(EntityIndexingFailureContext context);

	/**
	 * When this handler is used for handling mass indexing failures - returns the number of failures during
	 * one mass indexing beyond which the failure handler will no longer be notified. This threshold is reached
	 * separately for each indexed type. Otherwise, i.e. not in the context of mass indexing, this value is ignored.
	 * <p>
	 * May be overridden by mass indexer parameters
	 * (see {@code failureFloodingThreshold(long)} in the {@code MassIndexer} interface).
	 */
	@Incubating
	default long failureFloodingThreshold() {
		return Long.MAX_VALUE;
	}
}
