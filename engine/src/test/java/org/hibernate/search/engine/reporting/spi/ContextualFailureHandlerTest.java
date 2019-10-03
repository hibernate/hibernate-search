/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.newCapture;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.IndexFailureContext;

import org.junit.Before;
import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;

public class ContextualFailureHandlerTest extends EasyMockSupport {

	private FailureHandler failureHandlerMock;

	private Object workInfo1;
	private Object workInfo2;
	private Object workInfo3;

	@Before
	public void initMocks() {
		failureHandlerMock = createStrictMock( FailureHandler.class );

		workInfo1 = workInfo( 1 );
		workInfo2 = workInfo( 2 );
		workInfo3 = workInfo( 3 );
	}

	@Test
	public void singleError() {
		Capture<IndexFailureContext> capture = newCapture();

		replayAll();
		ContextualFailureHandler handler =
				new ContextualFailureHandler( failureHandlerMock );
		verifyAll();

		Throwable throwable = new Throwable();

		resetAll();
		replayAll();
		handler.markAsFailed( workInfo1, throwable );
		handler.markAsSkipped( workInfo2 );
		handler.markAsSkipped( workInfo3 );
		verifyAll();

		resetAll();
		failureHandlerMock.handle( capture( capture ) );
		replayAll();
		handler.handle();
		verifyAll();
		IndexFailureContext failureContext = capture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( throwable );
		assertThat( failureContext.getFailingOperation() ).isSameAs( workInfo1 );
		assertThat( failureContext.getUncommittedOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	@Test
	public void noError() {
		replayAll();
		ContextualFailureHandler handler =
				new ContextualFailureHandler( failureHandlerMock );
		verifyAll();

		resetAll();
		replayAll();
		handler.handle();
		verifyAll();
	}

	@Test
	public void nonWorkError() {
		Capture<IndexFailureContext> capture = newCapture();

		replayAll();
		ContextualFailureHandler handler =
				new ContextualFailureHandler( failureHandlerMock );
		verifyAll();

		Throwable throwable = new Throwable();

		resetAll();
		replayAll();
		handler.markAsSkipped( workInfo1 );
		handler.markAsSkipped( workInfo2 );
		handler.markAsSkipped( workInfo3 );
		verifyAll();

		resetAll();
		replayAll();
		handler.addThrowable( throwable );
		verifyAll();

		resetAll();
		failureHandlerMock.handle( capture( capture ) );
		replayAll();
		handler.handle();
		verifyAll();
		IndexFailureContext failureContext = capture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( throwable );
		assertThat( failureContext.getFailingOperation() ).isNull();
		assertThat( failureContext.getUncommittedOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	@Test
	public void multipleErrors_works() {
		Capture<IndexFailureContext> capture = newCapture();

		replayAll();
		ContextualFailureHandler handler =
				new ContextualFailureHandler( failureHandlerMock );
		verifyAll();

		Throwable throwable1 = new Throwable();
		Throwable throwable2 = new Throwable();

		resetAll();
		replayAll();
		handler.markAsFailed( workInfo1, throwable1 );
		handler.markAsFailed( workInfo2, throwable2 );
		handler.markAsSkipped( workInfo3 );
		verifyAll();

		resetAll();
		failureHandlerMock.handle( capture( capture ) );
		replayAll();
		handler.handle();
		verifyAll();
		IndexFailureContext failureContext = capture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( throwable1 );
		assertThat( throwable1.getSuppressed() ).containsExactlyInAnyOrder( throwable2 );
		assertThat( failureContext.getFailingOperation() ).isIn( workInfo1, workInfo2 );
		assertThat( failureContext.getUncommittedOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	@Test
	public void multipleErrors_workAndNotWork() {
		Capture<IndexFailureContext> capture = newCapture();

		replayAll();
		ContextualFailureHandler handler =
				new ContextualFailureHandler( failureHandlerMock );
		verifyAll();

		Throwable throwable1 = new Throwable();
		Throwable throwable2 = new Throwable();

		resetAll();
		replayAll();
		handler.markAsFailed( workInfo1, throwable1 );
		handler.markAsSkipped( workInfo2 );
		handler.markAsSkipped( workInfo3 );
		verifyAll();

		resetAll();
		replayAll();
		handler.addThrowable( throwable2 );
		verifyAll();

		resetAll();
		failureHandlerMock.handle( capture( capture ) );
		replayAll();
		handler.handle();
		verifyAll();
		IndexFailureContext failureContext = capture.getValue();
		assertThat( failureContext.getThrowable() ).isSameAs( throwable1 );
		assertThat( throwable1.getSuppressed() ).containsExactlyInAnyOrder( throwable2 );
		assertThat( failureContext.getFailingOperation() ).isSameAs( workInfo1 );
		assertThat( failureContext.getUncommittedOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	private Object workInfo(int index) {
		Object mock = createMock( "workInfo" + index, Object.class );
		return mock;
	}
}
