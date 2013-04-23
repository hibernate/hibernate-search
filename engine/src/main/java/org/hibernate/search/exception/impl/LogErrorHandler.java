/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
