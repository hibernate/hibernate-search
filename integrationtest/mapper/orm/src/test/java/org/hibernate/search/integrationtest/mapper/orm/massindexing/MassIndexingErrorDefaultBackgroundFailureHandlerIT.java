/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;

import org.apache.log4j.Level;

public class MassIndexingErrorDefaultBackgroundFailureHandlerIT extends AbstractMassIndexingErrorIT {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Override
	protected String getBackgroundFailureHandlerReference() {
		return null;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return null;
	}

	@Override
	protected void expectEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedError.class )
						.withMessage( exceptionMessage )
						.build(),
				failingOperationAsString,
				"Entities that could not be indexed correctly:",
				entityReferenceAsString
		)
				.once();
	}

	@Override
	protected void assertEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// If we get there, everything works fine.
	}

	@Override
	protected void expectEntityGetterFailureHandling(String exceptionMessage, String failingOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedError.class )
						.withMessage( exceptionMessage )
						.build(),
				failingOperationAsString
		)
				.times( 2 );
	}

	@Override
	protected void assertEntityGetterFailureHandling(String exceptionMessage, String failingOperationAsString) {
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
	protected void expectEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedError.class )
						.withMessage( failingEntityIndexingExceptionMessage )
						.build(),
				failingEntityIndexingOperationAsString,
				"Entities that could not be indexed correctly:",
				entityReferenceAsString
		)
				.once();

		logged.expectEvent(
				Level.ERROR,
				ExceptionMatcherBuilder.isException( SimulatedError.class )
						.withMessage( failingMassIndexerOperationExceptionMessage )
						.build(),
				failingMassIndexerOperationAsString
		)
				.once();
	}

	@Override
	protected void assertEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		// If we get there, everything works fine.
	}
}
