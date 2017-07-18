/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class DefaultContextualErrorHandlerTest {

	private final List<Object> mocks = new ArrayList<>();

	private ErrorHandler errorHandlerMock;

	private ElasticsearchWork<?> work1;
	private ElasticsearchWork<?> work2;
	private ElasticsearchWork<?> work3;

	private LuceneWork luceneWork1;
	private LuceneWork luceneWork2;
	private LuceneWork luceneWork3;

	@Before
	public void initMocks() {
		errorHandlerMock = EasyMock.createStrictMock( ErrorHandler.class );
		mocks.add( errorHandlerMock );

		work1 = work( 1 );
		work2 = work( 2 );
		work3 = work( 3 );

		luceneWork1 = luceneWork( 1 );
		luceneWork2 = luceneWork( 2 );
		luceneWork3 = luceneWork( 3 );
	}

	@Test
	public void singleError() {
		Capture<ErrorContext> capture = new Capture<>();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable = new Throwable();

		reset();
		expect( work1.getLuceneWork() ).andReturn( luceneWork1 );
		expect( work2.getLuceneWork() ).andReturn( luceneWork2 );
		expect( work3.getLuceneWork() ).andReturn( luceneWork3 );
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
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork1 );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork1, luceneWork2, luceneWork3 );
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
		Capture<ErrorContext> capture = new Capture<>();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable = new Throwable();

		reset();
		expect( work1.getLuceneWork() ).andReturn( luceneWork1 );
		expect( work2.getLuceneWork() ).andReturn( luceneWork2 );
		expect( work3.getLuceneWork() ).andReturn( luceneWork3 );
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
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork1, luceneWork2, luceneWork3 );
	}

	@Test
	public void multipleErrors_works() {
		Capture<ErrorContext> capture = new Capture<>();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable1 = new Throwable();
		Throwable throwable2 = new Throwable();

		reset();
		expect( work1.getLuceneWork() ).andReturn( luceneWork1 );
		expect( work2.getLuceneWork() ).andReturn( luceneWork2 );
		expect( work3.getLuceneWork() ).andReturn( luceneWork3 );
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
		assertThat( throwable1.getSuppressed() ).containsOnly( throwable2 );
		assertThat( errorContext.getOperationAtFault() ).isIn( luceneWork1, luceneWork2 );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork1, luceneWork2, luceneWork3 );
	}

	@Test
	public void multipleErrors_workAndNotWork() {
		Capture<ErrorContext> capture = new Capture<>();

		replay();
		DefaultContextualErrorHandler handler =
				new DefaultContextualErrorHandler( errorHandlerMock );
		verify();

		Throwable throwable1 = new Throwable();
		Throwable throwable2 = new Throwable();

		reset();
		expect( work1.getLuceneWork() ).andReturn( luceneWork1 );
		expect( work2.getLuceneWork() ).andReturn( luceneWork2 );
		expect( work3.getLuceneWork() ).andReturn( luceneWork3 );
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
		assertThat( throwable1.getSuppressed() ).containsOnly( throwable2 );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork1 );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork1, luceneWork2, luceneWork3 );
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

	private LuceneWork luceneWork(int index) {
		LuceneWork mock = createMock( "luceneWork" + index, LuceneWork.class );
		mocks.add( mock );
		return mock;
	}
}
