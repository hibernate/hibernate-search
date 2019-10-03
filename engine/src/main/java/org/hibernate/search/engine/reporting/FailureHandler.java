/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

/**
 * Implementations handle errors and exceptions occurring during indexing.
 *
 * @author Amin Mohammed-Coleman
 * @since 3.2
 */
public interface FailureHandler {

	void handle(FailureContext context);

	/**
	 * Suited to handle a single Exception, where no FailureContext is needed.
	 * @since 4.0
	 * @param errorMsg any description which could be useful to identify what was happening
	 * @param exception the error to be handled
	 */
	void handleException(String errorMsg, Throwable exception);

	default ContextualFailureHandler createContextualHandler() {
		return new DefaultContextualFailureHandler( this );
	}

}
