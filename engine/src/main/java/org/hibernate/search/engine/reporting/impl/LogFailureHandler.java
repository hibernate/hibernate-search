/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.Log;

/**
 * @author Amin Mohammed-Coleman
 * @author Sanne Grinovero
 * @since 3.2
 */
public class LogFailureHandler implements FailureHandler {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void handle(IndexFailureContext context) {
		final List<Object> uncommittedOperations = context.getUncommittedOperations();
		final Object failingOperation = context.getFailingOperation();
		final Throwable throwable = context.getThrowable();

		final StringBuilder errorMsg = new StringBuilder();

		if ( throwable != null ) {
			errorMsg.append( "Exception occurred " )
				.append( throwable )
				.append( "\n" );
		}
		if ( failingOperation != null ) {
			errorMsg.append( "Failing operation:\n" );
			appendFailureMessage( errorMsg, failingOperation );
		}

		if ( ! uncommittedOperations.isEmpty() ) {
			errorMsg.append( "Uncommitted operations as a result:\n" );
			for ( Object workThatFailed : uncommittedOperations ) {
				appendFailureMessage( errorMsg, workThatFailed );
			}
		}

		handleException( errorMsg.toString(), throwable );
	}

	private static void appendFailureMessage(StringBuilder message, Object workThatFailed) {
		message.append( workThatFailed.toString() );
	}

	@Override
	public void handleException(String errorMsg, Throwable exception) {
		log.exceptionOccurred( errorMsg, exception );
	}

}
