/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import org.hibernate.search.mapper.orm.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hamcrest.MatcherAssert;

public class MassIndexingFailureCustomMassIndexingFailureHandlerIT extends AbstractMassIndexingFailureIT {

	private final MassIndexingFailureHandler failureHandler = EasyMock.createMock( MassIndexingFailureHandler.class );
	private final Capture<MassIndexingFailureContext> genericFailureContextCapture = EasyMock.newCapture();
	private final Capture<MassIndexingEntityFailureContext> entityFailureContextCapture = EasyMock.newCapture();

	@Override
	protected String getBackgroundFailureHandlerReference() {
		return null;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return failureHandler;
	}

	@Override
	protected void expectEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		reset( failureHandler );
		failureHandler.handle( capture( entityFailureContextCapture ) );
		replay( failureHandler );
	}

	@Override
	protected void assertEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler );

		MassIndexingEntityFailureContext context = entityFailureContextCapture.getValue();
		MatcherAssert.assertThat(
				context.throwable(),
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( exceptionMessage )
						.build()
		);
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
		assertThat( context.entityReferences() )
				.hasSize( 1 )
				.element( 0 )
				.asString()
				.isEqualTo( entityReferenceAsString );
	}

	@Override
	protected void expectEntityGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		reset( failureHandler );
		failureHandler.handle( capture( entityFailureContextCapture ) );
		replay( failureHandler );
	}

	@Override
	protected void assertEntityGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler );

		MassIndexingEntityFailureContext context = entityFailureContextCapture.getValue();
		MatcherAssert.assertThat(
				context.throwable(),
				ExceptionMatcherBuilder.isException( SearchException.class )
						.withMessage( "Exception while invoking" )
						.causedBy( SimulatedFailure.class )
						.withMessage( exceptionMessage )
						.build()
		);
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
		assertThat( context.entityReferences() )
				.hasSize( 1 )
				.element( 0 )
				.asString()
				.isEqualTo( entityReferenceAsString );
	}

	@Override
	protected void expectMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString) {
		reset( failureHandler );
		failureHandler.handle( capture( genericFailureContextCapture ) );
		replay( failureHandler );
	}

	@Override
	protected void assertMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString) {
		verify( failureHandler );

		MassIndexingFailureContext context = genericFailureContextCapture.getValue();
		MatcherAssert.assertThat(
				context.throwable(),
				ExceptionMatcherBuilder.isException( exceptionType )
						.withMessage( exceptionMessage )
						.build()
		);
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
	}

	@Override
	protected void expectEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		reset( failureHandler );
		failureHandler.handle( capture( entityFailureContextCapture ) );
		failureHandler.handle( capture( genericFailureContextCapture ) );
		replay( failureHandler );
	}

	@Override
	protected void assertEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		verify( failureHandler );

		MassIndexingEntityFailureContext entityFailureContext = entityFailureContextCapture.getValue();
		MatcherAssert.assertThat(
				entityFailureContext.throwable(),
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( failingEntityIndexingExceptionMessage )
						.build()
		);
		assertThat( entityFailureContext.failingOperation() ).asString()
				.isEqualTo( failingEntityIndexingOperationAsString );
		assertThat( entityFailureContext.entityReferences() )
				.hasSize( 1 )
				.element( 0 )
				.asString()
				.isEqualTo( entityReferenceAsString );


		MassIndexingFailureContext massIndexerOperationFailureContext = genericFailureContextCapture.getValue();
		MatcherAssert.assertThat(
				massIndexerOperationFailureContext.throwable(),
				ExceptionMatcherBuilder.isException( SimulatedFailure.class )
						.withMessage( failingMassIndexerOperationExceptionMessage )
						.build()
		);
		assertThat( massIndexerOperationFailureContext.failingOperation() ).asString()
				.isEqualTo( failingMassIndexerOperationAsString );
	}
}
