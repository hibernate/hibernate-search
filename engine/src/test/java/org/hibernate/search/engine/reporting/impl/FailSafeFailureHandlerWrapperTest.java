/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.sameInstance;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;
import org.easymock.EasyMockSupport;

public class FailSafeFailureHandlerWrapperTest extends EasyMockSupport {

	private final FailureHandler failureHandlerMock = createMock( FailureHandler.class );

	private final FailSafeFailureHandlerWrapper wrapper =
			new FailSafeFailureHandlerWrapper( failureHandlerMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void genericContext_runtimeException() {
		RuntimeException failure = new SimulatedRuntimeException();

		logged.expectEvent(
				Level.ERROR, sameInstance( failure ), "failure handler threw an exception"
		);

		resetAll();
		failureHandlerMock.handle( anyObject( FailureContext.class ) );
		expectLastCall().andThrow( failure );
		replayAll();
		wrapper.handle( FailureContext.builder().build() );
		verifyAll();
	}

	@Test
	public void genericContext_error() {
		Error failure = new SimulatedError();

		logged.expectEvent(
				Level.ERROR, sameInstance( failure ), "failure handler threw an exception"
		);

		resetAll();
		failureHandlerMock.handle( anyObject( FailureContext.class ) );
		expectLastCall().andThrow( failure );
		replayAll();
		wrapper.handle( FailureContext.builder().build() );
		verifyAll();
	}

	@Test
	public void entityIndexingContext_runtimeException() {
		RuntimeException failure = new SimulatedRuntimeException();

		logged.expectEvent(
				Level.ERROR, sameInstance( failure ), "failure handler threw an exception"
		);

		resetAll();
		failureHandlerMock.handle( anyObject( EntityIndexingFailureContext.class ) );
		expectLastCall().andThrow( failure );
		replayAll();
		wrapper.handle( EntityIndexingFailureContext.builder().build() );
		verifyAll();
	}

	@Test
	public void entityIndexingContext_error() {
		Error failure = new SimulatedError();

		logged.expectEvent(
				Level.ERROR, sameInstance( failure ), "failure handler threw an exception"
		);

		resetAll();
		failureHandlerMock.handle( anyObject( EntityIndexingFailureContext.class ) );
		expectLastCall().andThrow( failure );
		replayAll();
		wrapper.handle( EntityIndexingFailureContext.builder().build() );
		verifyAll();
	}

	private static class SimulatedError extends Error {
	}

	private static class SimulatedRuntimeException extends RuntimeException {
	}
}