/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.exception.impl;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

/**
 * @author Amin Mohammed-Coleman
 * @author Sanne Grinovero
 * @since 3.2
 */
public class LogErrorHandler implements ErrorHandler {

	private static final Log log = LoggerFactory.make();

	@Override
	public void handle(ErrorContext context) {

		final List<LuceneWork> failingOperations = context.getFailingOperations();
		final LuceneWork primaryFailure = context.getOperationAtFault();
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
			for ( LuceneWork workThatFailed : failingOperations ) {
				appendFailureMessage( errorMsg, workThatFailed );
			}
		}

		handleException( errorMsg.toString(), exceptionThatOccurred );
	}

	public static final void appendFailureMessage(StringBuilder message, LuceneWork workThatFailed) {
		message.append( "\tEntity " )
			.append( workThatFailed.getEntityClass().getName() )
			.append( " " )
			.append( " Id " ).append( workThatFailed.getIdInString() )
			.append( " " ).append( " Work Type " )
			.append( " " ).append( workThatFailed.getClass().getName() )
			.append( "\n" );
	}

	@Override
	public void handleException(String errorMsg, Throwable exception) {
		log.exceptionOccurred( errorMsg, exception );
	}

}
