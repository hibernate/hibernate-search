/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.impl;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class FailSafeFailureHandlerWrapperTest {

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Mock
	private FailureHandler failureHandlerMock;

	private FailSafeFailureHandlerWrapper wrapper;

	@BeforeEach
	void setup() {
		wrapper = new FailSafeFailureHandlerWrapper( failureHandlerMock );
	}

	@Test
	void genericContext_runtimeException() {
		RuntimeException runtimeException = new SimulatedRuntimeException();

		logged.expectEvent(
				Level.ERROR, sameInstance( runtimeException ), "failure handler threw an exception"
		);

		doThrow( runtimeException ).when( failureHandlerMock ).handle( any( FailureContext.class ) );
		wrapper.handle( FailureContext.builder().build() );
		verifyNoMoreInteractions( failureHandlerMock );
	}

	@Test
	void genericContext_error() {
		Error error = new SimulatedError();

		logged.expectEvent(
				Level.ERROR, sameInstance( error ), "failure handler threw an exception"
		);

		doThrow( error ).when( failureHandlerMock ).handle( any( FailureContext.class ) );
		wrapper.handle( FailureContext.builder().build() );
		verifyNoMoreInteractions( failureHandlerMock );
	}

	@Test
	void entityIndexingContext_runtimeException() {
		RuntimeException runtimeException = new SimulatedRuntimeException();

		logged.expectEvent(
				Level.ERROR, sameInstance( runtimeException ), "failure handler threw an exception"
		);

		doThrow( runtimeException ).when( failureHandlerMock ).handle( any( EntityIndexingFailureContext.class ) );
		wrapper.handle( EntityIndexingFailureContext.builder().build() );
		verifyNoMoreInteractions( failureHandlerMock );
	}

	@Test
	void entityIndexingContext_error() {
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
