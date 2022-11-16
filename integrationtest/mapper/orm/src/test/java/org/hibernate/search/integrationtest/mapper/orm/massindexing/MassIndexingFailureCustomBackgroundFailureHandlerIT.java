/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.extension.ExtendWith;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@ExtendWith(MockitoExtension.class)
public class MassIndexingFailureCustomBackgroundFailureHandlerIT extends AbstractMassIndexingFailureIT {

	@Mock
	private FailureHandler failureHandler;

	@Captor
	private ArgumentCaptor<FailureContext> genericFailureContextCapture;
	@Captor
	private ArgumentCaptor<EntityIndexingFailureContext> entityFailureContextCapture;

	@Override
	protected FailureHandler getBackgroundFailureHandlerReference() {
		return failureHandler;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return null;
	}

	@Override
	protected void expectEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verifyNoMoreInteractions( failureHandler );

		EntityIndexingFailureContext context = entityFailureContextCapture.getValue();
		assertThat( context.throwable() )
				.isInstanceOf( SimulatedFailure.class )
				.hasMessage( exceptionMessage );
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
		assertThat( context.entityReferences() )
				.hasSize( 1 )
				.element( 0 )
				.asString()
				.isEqualTo( entityReferenceAsString );
	}

	@Override
	protected void expectEntityIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verifyNoMoreInteractions( failureHandler );

		EntityIndexingFailureContext context = entityFailureContextCapture.getValue();
		assertThat( context.throwable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Exception while invoking" )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SimulatedFailure.class )
				.hasMessageContaining( exceptionMessage );
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
		assertThat( context.entityReferences() )
				.hasSize( 1 )
				.element( 0 )
				.asString()
				.isEqualTo( entityReferenceAsString );
	}

	@Override
	protected void expectEntityNonIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityNonIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verifyNoMoreInteractions( failureHandler );

		EntityIndexingFailureContext context = entityFailureContextCapture.getValue();
		assertThat( context.throwable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Exception while building document for entity '" + entityReferenceAsString + "'",
						"Exception while invoking",
						exceptionMessage )
				.hasRootCauseInstanceOf( SimulatedFailure.class );
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
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString) {
		verify( failureHandler ).handle( genericFailureContextCapture.capture() );
		verifyNoMoreInteractions( failureHandler );

		FailureContext context = genericFailureContextCapture.getValue();
		assertThat( context.throwable() )
				.isInstanceOf( exceptionType )
				.hasMessageContaining( exceptionMessage );
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
	}

	@Override
	protected void expectEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verify( failureHandler ).handle( genericFailureContextCapture.capture() );
		verifyNoMoreInteractions( failureHandler );

		EntityIndexingFailureContext entityFailureContext = entityFailureContextCapture.getValue();
		assertThat( entityFailureContext.throwable() )
				.isInstanceOf( SimulatedFailure.class )
				.hasMessage( failingEntityIndexingExceptionMessage );
		assertThat( entityFailureContext.failingOperation() ).asString()
				.isEqualTo( failingEntityIndexingOperationAsString );
		assertThat( entityFailureContext.entityReferences() )
				.hasSize( 1 )
				.element( 0 )
				.asString()
				.isEqualTo( entityReferenceAsString );

		FailureContext massIndexerOperationFailureContext = genericFailureContextCapture.getValue();
		assertThat( massIndexerOperationFailureContext.throwable() )
				.isInstanceOf( SimulatedFailure.class )
				.hasMessage( failingMassIndexerOperationExceptionMessage );
		assertThat( massIndexerOperationFailureContext.failingOperation() ).asString()
				.isEqualTo( failingMassIndexerOperationAsString );
	}
}
