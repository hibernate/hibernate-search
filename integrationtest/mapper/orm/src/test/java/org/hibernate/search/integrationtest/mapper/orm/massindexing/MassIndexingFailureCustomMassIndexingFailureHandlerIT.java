/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Rule;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class MassIndexingFailureCustomMassIndexingFailureHandlerIT extends AbstractMassIndexingFailureIT {

	private static final int DEFAULT_FAILURE_FLOODING_THRESHOLD = 62;

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private MassIndexingFailureHandler failureHandler;

	@Captor
	private ArgumentCaptor<MassIndexingFailureContext> genericFailureContextCapture;
	@Captor
	private ArgumentCaptor<MassIndexingEntityFailureContext> entityFailureContextCapture;

	@Override
	protected FailureHandler getBackgroundFailureHandlerReference() {
		return null;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return failureHandler;
	}

	@Override
	protected void expectEntityIndexingFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIndexingFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verify( failureHandler ).failureFloodingThreshold();
		verifyNoMoreInteractions( failureHandler );

		MassIndexingEntityFailureContext context = entityFailureContextCapture.getValue();
		assertSingleEntityFailure( context, entityReference, failingOperationAsString,
				e -> assertThat( e )
				.isInstanceOf( SimulatedFailure.class )
						.hasMessage( exceptionMessage ) );
	}

	@Override
	protected void expectEntityIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verify( failureHandler ).failureFloodingThreshold();
		verifyNoMoreInteractions( failureHandler );

		MassIndexingEntityFailureContext context = entityFailureContextCapture.getValue();
		assertSingleEntityFailure( context, entityReference, failingOperationAsString,
				e -> assertThat( e )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Exception while invoking" )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ) );
	}

	@Override
	protected void expectEntityNonIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityNonIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verify( failureHandler ).failureFloodingThreshold();
		verifyNoMoreInteractions( failureHandler );

		MassIndexingEntityFailureContext context = entityFailureContextCapture.getValue();
		assertSingleEntityFailure( context, entityReference, failingOperationAsString,
				e -> assertThat( e )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Exception while building document for entity '" + entityReference + "'",
						"Exception while invoking",
						exceptionMessage )
						.hasRootCauseInstanceOf( SimulatedFailure.class ) );
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
		verify( failureHandler ).failureFloodingThreshold();
		verifyNoMoreInteractions( failureHandler );

		MassIndexingFailureContext context = genericFailureContextCapture.getValue();
		assertThat( context.throwable() )
				.isInstanceOf( exceptionType )
				.hasMessageContaining( exceptionMessage );
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
	}

	@Override
	protected void expectMassIndexerLoadingOperationFailureHandling(Class<? extends Throwable> exceptionType,
			String exceptionMessage, int count, String failingOperationAsString, String... extraMessages) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertMassIndexerLoadingOperationFailureHandling(Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString,
			int failureFloodingThreshold, Class<? extends Throwable> closingExceptionType,
			String closingExceptionMessage, String closingFailingOperationAsString) {
		verify( failureHandler, times( failureFloodingThreshold ) ).handle( entityFailureContextCapture.capture() );

		MassIndexingEntityFailureContext context = entityFailureContextCapture.getValue();
		assertSingleEntityFailure( context, null, failingOperationAsString,
				e -> assertThat( e )
				.isInstanceOf( SimulatedFailure.class )
						.hasMessageContainingAll( exceptionMessage ) );

		verify( failureHandler, times( 1 ) ).handle( genericFailureContextCapture.capture() );

		MassIndexingFailureContext genericContext = genericFailureContextCapture.getValue();
		assertThat( genericContext.throwable() )
				.isInstanceOf( closingExceptionType )
				.hasMessageContainingAll( closingExceptionMessage );
		assertThat( genericContext.failingOperation() ).asString()
				.isEqualTo( closingFailingOperationAsString );

		if ( failureFloodingThreshold == getDefaultFailureFloodingThreshold() ) {
			verify( failureHandler ).failureFloodingThreshold();
		}

		verifyNoMoreInteractions( failureHandler );
	}

	@Override
	protected void expectEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			EntityReference entityReference,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		// We'll check in the assert*() method, see below.
	}

	@Override
	protected void assertEntityIndexingAndMassIndexerOperationFailureHandling(String entityName,
			EntityReference entityReference,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString) {
		verify( failureHandler ).handle( entityFailureContextCapture.capture() );
		verify( failureHandler ).handle( genericFailureContextCapture.capture() );
		verify( failureHandler ).failureFloodingThreshold();
		verifyNoMoreInteractions( failureHandler );

		MassIndexingEntityFailureContext context = entityFailureContextCapture.getValue();
		assertSingleEntityFailure( context, entityReference, failingEntityIndexingOperationAsString,
				e -> assertThat( e )
				.isInstanceOf( SimulatedFailure.class )
						.hasMessage( failingEntityIndexingExceptionMessage ) );

		MassIndexingFailureContext massIndexerOperationFailureContext = genericFailureContextCapture.getValue();
		assertThat( massIndexerOperationFailureContext.throwable() )
				.isInstanceOf( SimulatedFailure.class )
				.hasMessage( failingMassIndexerOperationExceptionMessage );
		assertThat( massIndexerOperationFailureContext.failingOperation() ).asString()
				.isEqualTo( failingMassIndexerOperationAsString );
	}

	@Before
	public void setUp() {
		lenient().when( failureHandler.failureFloodingThreshold() ).thenReturn(
				(long) getDefaultFailureFloodingThreshold() );
	}

	@Override
	public int getDefaultFailureFloodingThreshold() {
		return DEFAULT_FAILURE_FLOODING_THRESHOLD;
	}

	private static void assertSingleEntityFailure(MassIndexingEntityFailureContext context, EntityReference entityReference,
			String failingOperationAsString,
			Consumer<Throwable> throwableAssertion) {
		assertThat( context.throwable() )
				.satisfies( throwableAssertion );
		assertThat( context.failingOperation() ).asString()
				.isEqualTo( failingOperationAsString );
		assertThat( context.failingEntityReferences() )
				.hasSize( 1 );
		// Also check the legacy method
		@SuppressWarnings("deprecation")
		List<Object> legacyReferences = context.entityReferences();
		assertThat( legacyReferences )
				.hasSize( 1 );

		if ( entityReference != null ) {
			assertThat( context.failingEntityReferences() )
					.element( 0 )
					.isEqualTo( entityReference );
			assertThat( legacyReferences )
					.element( 0 )
					.asString()
					.isEqualTo( entityReference.toString() );
		}
	}
}
