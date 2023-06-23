/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;

import org.apache.logging.log4j.Level;

public class MassIndexingFailureDefaultBackgroundFailureHandlerIT extends AbstractMassIndexingFailureIT {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Override
	protected FailureHandler getBackgroundFailureHandlerReference() {
		return null;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return null;
	}

	@Override
	protected void expectEntityIndexingFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( exceptionMessage )
						.build(),
				failingOperationAsString,
				"Entities that could not be indexed correctly:",
				entityReference.toString()
		)
				.once();
	}

	@Override
	protected void assertEntityIndexingFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		// If we get there, everything works fine.
	}

	@Override
	protected void expectEntityIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Exception while invoking" )
						.causedBy( SimulatedFailure.class )
						.withMessage( exceptionMessage )
						.build(),
				failingOperationAsString,
				"Entities that could not be indexed correctly:",
				entityReference.toString()
		)
				.once();
	}

	@Override
	protected void assertEntityNonIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		// If we get there, everything works fine.
	}

	@Override
	protected void expectEntityNonIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Exception while building document for entity '" + entityReference + "'" )
						.withMessage( "Exception while invoking" )
						.withMessage( exceptionMessage )
						.rootCause( SimulatedFailure.class )
						.build(),
				failingOperationAsString,
				"Entities that could not be indexed correctly:",
				entityReference.toString()
		)
				.once();
	}

	@Override
	protected void assertEntityIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		// If we get there, everything works fine.
	}

	@Override
	protected void expectMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( exceptionType )
						.withMessage( exceptionMessage )
						.build(),
				failingOperationAsString
		)
				.once();
	}

	@Override
	protected void assertMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString) {
		// If we get there, everything works fine.
	}

	@Override
	protected void expectMassIndexerLoadingOperationFailureHandling(Class<? extends Throwable> exceptionType,
			String exceptionMessage, int count, String failingOperationAsString, String... extraMessages) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( exceptionType )
						.withMessage( exceptionMessage )
						.build(),
				failingOperationAsString,
				extraMessages
		)
				.times( count );
	}

	@Override
	protected void assertMassIndexerLoadingOperationFailureHandling(Class<? extends Throwable> exceptionType,
			String exceptionMessage,
			String failingOperationAsString,
			int failureFloodingThreshold, Class<? extends Throwable> closingExceptionType,
			String closingExceptionMessage, String closingFailingOperationAsString) {
		// If we get there, everything works fine.
	}

	@Override
	protected void expectEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			EntityReference entityReference,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( failingEntityIndexingExceptionMessage )
						.build(),
				failingEntityIndexingOperationAsString,
				"Entities that could not be indexed correctly:",
				entityReference.toString()
		)
				.once();

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( failingMassIndexerOperationExceptionMessage )
						.build(),
				failingMassIndexerOperationAsString
		)
				.once();
	}

	@Override
	protected void assertEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			EntityReference entityReference,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		// If we get there, everything works fine.
	}
}
