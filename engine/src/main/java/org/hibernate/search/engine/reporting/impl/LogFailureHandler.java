/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
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
	public void handle(FailureContext context) {
		log.exceptionOccurred( formatMessage( context ).toString(), context.getThrowable() );
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		log.exceptionOccurred( formatMessage( context ).toString(), context.getThrowable() );
	}

	@Override
	public void handle(IndexFailureContext context) {
		log.exceptionOccurred( formatMessage( context ).toString(), context.getThrowable() );
	}

	private StringBuilder formatMessage(FailureContext context) {
		final Throwable throwable = context.getThrowable();
		final Object failingOperation = context.getFailingOperation();

		final StringBuilder messageBuilder = new StringBuilder();

		messageBuilder.append( "Exception occurred " )
				.append( throwable )
				.append( "\n" );
		messageBuilder.append( "Failing operation:\n" );
		messageBuilder.append( failingOperation );
		messageBuilder.append( "\n" );

		return messageBuilder;
	}

	private StringBuilder formatMessage(EntityIndexingFailureContext context) {
		final List<?> entityReferences = context.getEntityReferences();

		final StringBuilder messageBuilder = formatMessage( (FailureContext) context );

		if ( ! entityReferences.isEmpty() ) {
			messageBuilder.append( "Entities that could not be indexed correctly:\n" );
			for ( Object entityReference : entityReferences ) {
				messageBuilder.append( entityReference );
				messageBuilder.append( " " );
			}
		}

		return messageBuilder;
	}

	private StringBuilder formatMessage(IndexFailureContext context) {
		final List<?> uncommittedOperations = context.getUncommittedOperations();

		final StringBuilder messageBuilder = formatMessage( (FailureContext) context );

		if ( ! uncommittedOperations.isEmpty() ) {
			messageBuilder.append( "Uncommitted operations as a result:\n" );
			for ( Object operation : uncommittedOperations ) {
				messageBuilder.append( operation );
				messageBuilder.append( "\n" );
			}
		}

		return messageBuilder;
	}

}
