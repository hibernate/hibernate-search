/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

/**
 * A handler for failures occurring during background operations,
 * which may not have been reported to the caller due to being executed asynchronously.
 * <p>
 * The handler should be used to report failures to application maintainers.
 * The default failure handler simply logs failures at the {@code ERROR} level,
 * but it can be replaced with a custom implementations through
 * {@link org.hibernate.search.engine.cfg.EngineSettings#BACKGROUND_FAILURE_HANDLER a configuration property}.
 *
 * @author Amin Mohammed-Coleman
 */
public interface FailureHandler {

	/**
	 * Handle a generic failure.
	 *
	 * @param context Contextual information about the failure (throwable, operation, ...)
	 */
	void handle(FailureContext context);

	/**
	 * Handle the failure of an index operation.
	 *
	 * @param context Contextual information about the failure (throwable, operation, ...)
	 */
	void handle(IndexFailureContext context);

}
