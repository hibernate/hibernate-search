/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.impl;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class FailSafeFailureHandlerWrapperTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Mock
	private FailureHandler failureHandlerMock;

	private FailSafeFailureHandlerWrapper wrapper;

	@Before
	public void setup() {
		wrapper = new FailSafeFailureHandlerWrapper( failureHandlerMock );
	}

	@Test
	public void genericContext_runtimeException() {
		RuntimeException runtimeException = new SimulatedRuntimeException();

		logged.expectEvent(
				Level.ERROR, sameInstance( runtimeException ), "failure handler threw an exception"
		);

		doThrow( runtimeException ).when( failureHandlerMock ).handle( any( FailureContext.class ) );
		wrapper.handle( FailureContext.builder().build() );
		verifyNoMoreInteractions( failureHandlerMock );
	}

	@Test
	public void genericContext_error() {
		Error error = new SimulatedError();

		logged.expectEvent(
				Level.ERROR, sameInstance( error ), "failure handler threw an exception"
		);

		doThrow( error ).when( failureHandlerMock ).handle( any( FailureContext.class ) );
		wrapper.handle( FailureContext.builder().build() );
		verifyNoMoreInteractions( failureHandlerMock );
	}

	@Test
	public void entityIndexingContext_runtimeException() {
		RuntimeException runtimeException = new SimulatedRuntimeException();

		logged.expectEvent(
				Level.ERROR, sameInstance( runtimeException ), "failure handler threw an exception"
		);

		doThrow( runtimeException ).when( failureHandlerMock ).handle( any( EntityIndexingFailureContext.class ) );
		wrapper.handle( EntityIndexingFailureContext.builder().build() );
		verifyNoMoreInteractions( failureHandlerMock );
	}

	@Test
	public void entityIndexingContext_error() {
		Error error = new SimulatedError();

		logged.expectEvent(
				Level.ERROR, sameInstance( error ), "failure handler threw an exception"
		);


		doThrow( error ).when( failureHandlerMock ).handle( any( EntityIndexingFailureContext.class ) );
		wrapper.handle( EntityIndexingFailureContext.builder().build() );
		verifyNoMoreInteractions( failureHandlerMock );
	}

	private static class SimulatedError extends Error {
	}

	private static class SimulatedRuntimeException extends RuntimeException {
	}
}
