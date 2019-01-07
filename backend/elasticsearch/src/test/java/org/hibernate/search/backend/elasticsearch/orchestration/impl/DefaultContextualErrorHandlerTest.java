/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.createMock;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.common.spi.ErrorContext;
import org.hibernate.search.engine.common.spi.ErrorHandler;

import org.junit.Before;
import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMock;

/**
 * @author Yoann Rodiere
 */
public class DefaultContextualErrorHandlerTest {

	private final List<Object> mocks = new ArrayList<>();

	private ErrorHandler errorHandlerMock;

	private ElasticsearchWork<?> work1;
	private ElasticsearchWork<?> work2;
	private ElasticsearchWork<?> work3;

	private Object workInfo1;
	private Object workInfo2;
	private Object workInfo3;

	@Before
	public void initMocks() {
		errorHandlerMock = EasyMock.createStrictMock( ErrorHandler.class );
		mocks.add( errorHandlerMock );

		work1 = work( 1 );
		work2 = work( 2 );
		work3 = work( 3 );

		workInfo1 = workInfo( 1 );
		workInfo2 = workInfo( 2 );
		workInfo3 = workInfo( 3 );
	}

	@Test
	public void singleError() {
		Capture<ErrorContext> capture = newCapture();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable = new Throwable();

		reset();
		expect( work1.getInfo() ).andReturn( workInfo1 );
		expect( work2.getInfo() ).andReturn( workInfo2 );
		expect( work3.getInfo() ).andReturn( workInfo3 );
		replay();
		handler.markAsFailed( work1, throwable );
		handler.markAsSkipped( work2 );
		handler.markAsSkipped( work3 );
		verify();

		reset();
		errorHandlerMock.handle( capture( capture ) );
		replay();
		handler.handle();
		verify();
		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isSameAs( throwable );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( workInfo1 );
		assertThat( errorContext.getFailingOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	@Test
	public void noError() {
		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		reset();
		replay();
		handler.handle();
		verify();
	}

	@Test
	public void nonWorkError() {
		Capture<ErrorContext> capture = newCapture();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable = new Throwable();

		reset();
		expect( work1.getInfo() ).andReturn( workInfo1 );
		expect( work2.getInfo() ).andReturn( workInfo2 );
		expect( work3.getInfo() ).andReturn( workInfo3 );
		replay();
		handler.markAsSkipped( work1 );
		handler.markAsSkipped( work2 );
		handler.markAsSkipped( work3 );
		verify();

		reset();
		replay();
		handler.addThrowable( throwable );
		verify();

		reset();
		errorHandlerMock.handle( capture( capture ) );
		replay();
		handler.handle();
		verify();
		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isSameAs( throwable );
		assertThat( errorContext.getOperationAtFault() ).isNull();
		assertThat( errorContext.getFailingOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	@Test
	public void multipleErrors_works() {
		Capture<ErrorContext> capture = newCapture();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable1 = new Throwable();
		Throwable throwable2 = new Throwable();

		reset();
		expect( work1.getInfo() ).andReturn( workInfo1 );
		expect( work2.getInfo() ).andReturn( workInfo2 );
		expect( work3.getInfo() ).andReturn( workInfo3 );
		replay();
		handler.markAsFailed( work1, throwable1 );
		handler.markAsFailed( work2, throwable2 );
		handler.markAsSkipped( work3 );
		verify();

		reset();
		errorHandlerMock.handle( capture( capture ) );
		replay();
		handler.handle();
		verify();
		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isSameAs( throwable1 );
		assertThat( throwable1.getSuppressed() ).containsExactlyInAnyOrder( throwable2 );
		assertThat( errorContext.getOperationAtFault() ).isIn( workInfo1, workInfo2 );
		assertThat( errorContext.getFailingOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	@Test
	public void multipleErrors_workAndNotWork() {
		Capture<ErrorContext> capture = newCapture();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable1 = new Throwable();
		Throwable throwable2 = new Throwable();

		reset();
		expect( work1.getInfo() ).andReturn( workInfo1 );
		expect( work2.getInfo() ).andReturn( workInfo2 );
		expect( work3.getInfo() ).andReturn( workInfo3 );
		replay();
		handler.markAsFailed( work1, throwable1 );
		handler.markAsSkipped( work2 );
		handler.markAsSkipped( work3 );
		verify();

		reset();
		replay();
		handler.addThrowable( throwable2 );
		verify();

		reset();
		errorHandlerMock.handle( capture( capture ) );
		replay();
		handler.handle();
		verify();
		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isSameAs( throwable1 );
		assertThat( throwable1.getSuppressed() ).containsExactlyInAnyOrder( throwable2 );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( workInfo1 );
		assertThat( errorContext.getFailingOperations() ).containsExactlyInAnyOrder( workInfo1, workInfo2, workInfo3 );
	}

	private void reset() {
		EasyMock.reset( mocks.toArray() );
	}

	private void replay() {
		EasyMock.replay( mocks.toArray() );
	}

	private void verify() {
		EasyMock.verify( mocks.toArray() );
	}

	private ElasticsearchWork<?> work(int index) {
		ElasticsearchWork<?> mock = createMock( "work" + index, ElasticsearchWork.class );
		mocks.add( mock );
		return mock;
	}

	private Object workInfo(int index) {
		Object mock = createMock( "workInfo" + index, Object.class );
		mocks.add( mock );
		return mock;
	}
}
