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

		final List<Object> failingOperations = context.getFailingOperations();
		final Object primaryFailure = context.getOperationAtFault();
		final Throwable exceptionThatOccurred = context.getThrowable();

		final StringBuilder errorMsg = new StringBuilder();

		if ( exceptionThatOccurred != null ) {
			errorMsg.append( "Exception occurred " )
				.append( exceptionThatOccurred )
				.append( "\n" );
		}
		if ( primaryFailure != null ) {
			errorMsg.append( "Primary Failure:\n" );
			appendFailureMessage( errorMsg, primaryFailure );
		}

		if ( ! failingOperations.isEmpty() ) {
			errorMsg.append( "Subsequent failures:\n" );
			for ( Object workThatFailed : failingOperations ) {
				appendFailureMessage( errorMsg, workThatFailed );
			}
		}

		handleException( errorMsg.toString(), exceptionThatOccurred );
	}

	private static void appendFailureMessage(StringBuilder message, Object workThatFailed) {
		message.append( workThatFailed.toString() );
	}

	@Override
	public void handleException(String errorMsg, Throwable exception) {
		log.exceptionOccurred( errorMsg, exception );
	}

}
