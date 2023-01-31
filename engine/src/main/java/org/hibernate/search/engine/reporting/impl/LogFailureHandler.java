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
	private static final int FAILURE_FLOODING_THRESHOLD = 100;

	public static final String NAME = "log";

	@Override
	public void handle(FailureContext context) {
		log.exceptionOccurred( formatMessage( context ).toString(), context.throwable() );
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		log.exceptionOccurred( formatMessage( context ).toString(), context.throwable() );
	}

	@Override
	public long failureFloodingThreshold() {
		return FAILURE_FLOODING_THRESHOLD;
	}

	private StringBuilder formatMessage(FailureContext context) {
		final Throwable throwable = context.throwable();
		final Object failingOperation = context.failingOperation();

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
		final List<?> entityReferences = context.entityReferences();

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

}
