/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.work.impl.BulkRequestFailedException;
import org.hibernate.search.elasticsearch.work.impl.BulkWork;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkAggregator;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * Test error handling in {@link ElasticsearchWorkProcessor}.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexWorkProcessorErrorHandlingTest {

	private ErrorHandler errorHandlerMock;

	private ElasticsearchWorkProcessor processor;

	private Map<Integer, LuceneWork> luceneWorks = new HashMap<>();

	@Before
	public void setup() throws Exception {
		this.errorHandlerMock = createMock( ErrorHandler.class );

		BuildContext buildContextMock = createMock( BuildContext.class );
		ElasticsearchClient clientMock = createMock( ElasticsearchClient.class );
		GsonProvider gsonProviderMock = createMock( GsonProvider.class );
		ElasticsearchWorkFactory workFactoryMock = createMock( ElasticsearchWorkFactory.class );

		expect( buildContextMock.getErrorHandler() ).andReturn( errorHandlerMock );

		expect( clientMock.submit( anyObject() ) )
				.andReturn( CompletableFuture.completedFuture( new ElasticsearchResponse( 200, "OK", new JsonObject() ) ) )
				.anyTimes();

		expect( workFactoryMock.bulk( anyObject() ) ).andAnswer( () -> {
			@SuppressWarnings("unchecked")
			List<BulkableElasticsearchWork<?>> bulkableWorks =
					(List<BulkableElasticsearchWork<?>>) EasyMock.getCurrentArguments()[0];
			return new BulkWork.Builder( bulkableWorks );
		} ).anyTimes();

		replay( buildContextMock, clientMock, gsonProviderMock, workFactoryMock );

		this.processor = new ElasticsearchWorkProcessor( buildContextMock, clientMock, gsonProviderMock, workFactoryMock );
	}

	@After
	public void tearDown() {
		this.processor.close();
	}

	@Test
	public void syncSafe_single() throws Exception {
		Capture<ErrorContext> capture = new Capture<>();

		errorHandlerMock.handle( capture( capture ) );
		expectLastCall().once();

		ElasticsearchWork<?> work = work( 1 );
		expect( work.execute( anyObject() ) ).andReturn( failedFuture() );

		replay( errorHandlerMock, work );

		processor.executeSyncSafe( Arrays.asList( work ) );

		verify( errorHandlerMock, work );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( SearchException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 1 ) );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork( 1 ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2652")
	public void syncSafe_multiple_noBulk() throws Exception {
		Capture<ErrorContext> capture = new Capture<>();

		errorHandlerMock.handle( capture( capture ) );
		expectLastCall().once();

		ElasticsearchWork<?> work1 = work( 1 );
		ElasticsearchWork<?> work2 = work( 2 );
		ElasticsearchWork<?> work3 = work( 3 );
		ElasticsearchWork<?> work4 = work( 4 );
		expect( work1.execute( anyObject() ) ).andReturn( completedFuture() );
		expect( work2.execute( anyObject() ) ).andReturn( failedFuture() );

		replay( errorHandlerMock, work1, work2, work3, work4 );

		processor.executeSyncSafe( Arrays.asList( work1, work2, work3, work4 ) );

		verify( errorHandlerMock, work1, work2, work3, work4 );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( SearchException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 2 ) );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork( 2 ), luceneWork( 3 ), luceneWork( 4 ) );
	}

	@Test
	public void syncSafe_multiple_bulk() throws Exception {
		Capture<ErrorContext> capture = new Capture<>();

		errorHandlerMock.handle( capture( capture ) );
		expectLastCall().once();

		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<?> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<?> work4 = bulkableWork( 4 );
		expect( work1.handleBulkResult( anyObject(), anyObject() ) ).andReturn( true );
		expect( work2.handleBulkResult( anyObject(), anyObject() ) ).andReturn( true );
		expect( work3.handleBulkResult( anyObject(), anyObject() ) ).andReturn( false );
		expect( work4.handleBulkResult( anyObject(), anyObject() ) ).andReturn( true );

		replay( errorHandlerMock, work1, work2, work3, work4 );

		processor.executeSyncSafe( Arrays.asList( work1, work2, work3, work4 ) );

		verify( errorHandlerMock, work1, work2, work3, work4 );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( BulkRequestFailedException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 3 ) );
		// work4 shouldn't be marked as failed: bulk execution doesn't stop after a failure
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork( 3 ) );
	}

	@Test
	public void asyncItem() throws Exception {
		Capture<ErrorContext> capture = new Capture<>();

		errorHandlerMock.handle( capture( capture ) );
		expectLastCall().once();

		ElasticsearchWork<?> work = work( 1 );
		expect( work.execute( anyObject() ) ).andReturn( failedFuture() );

		replay( errorHandlerMock, work );

		processor.executeAsync( work );
		processor.awaitAsyncProcessingCompletion();

		verify( errorHandlerMock, work );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( SearchException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 1 ) );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork( 1 ) );
	}

	@Test
	public void asyncList_single() throws Exception {
		Capture<ErrorContext> capture = new Capture<>();

		errorHandlerMock.handle( capture( capture ) );
		expectLastCall().once();

		ElasticsearchWork<?> work = work( 1 );
		expect( work.execute( anyObject() ) ).andReturn( failedFuture() );

		replay( errorHandlerMock, work );

		processor.executeAsync( Arrays.asList( work ) );
		processor.awaitAsyncProcessingCompletion();

		verify( errorHandlerMock, work );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( SearchException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 1 ) );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork( 1 ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2652")
	public void asyncList_multiple_noBulk() throws Exception {
		Capture<ErrorContext> capture = new Capture<>();

		errorHandlerMock.handle( capture( capture ) );
		expectLastCall().once();

		ElasticsearchWork<?> work1 = work( 1 );
		ElasticsearchWork<?> work2 = work( 2 );
		ElasticsearchWork<?> work3 = work( 3 );
		ElasticsearchWork<?> work4 = work( 4 );
		expect( work1.execute( anyObject() ) ).andReturn( completedFuture() );
		expect( work2.execute( anyObject() ) ).andReturn( failedFuture() );

		replay( errorHandlerMock, work1, work2, work3, work4 );

		processor.executeAsync( Arrays.asList( work1, work2, work3, work4 ) );
		processor.awaitAsyncProcessingCompletion();

		verify( errorHandlerMock, work1, work2, work3, work4 );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( SearchException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 2 ) );
		assertThat( errorContext.getFailingOperations() )
				.containsOnly( luceneWork( 2 ), luceneWork( 3 ), luceneWork( 4 ) );
	}

	@Test
	public void asyncList_multiple_bulk() throws Exception {
		Capture<ErrorContext> capture = new Capture<>();

		errorHandlerMock.handle( capture( capture ) );
		expectLastCall().once();

		BulkableElasticsearchWork<?> work1 = bulkableWork( 1 );
		BulkableElasticsearchWork<?> work2 = bulkableWork( 2 );
		BulkableElasticsearchWork<?> work3 = bulkableWork( 3 );
		BulkableElasticsearchWork<?> work4 = bulkableWork( 4 );
		ElasticsearchWork<?> work5 = work( 5 );
		expect( work1.handleBulkResult( anyObject(), anyObject() ) ).andReturn( true );
		expect( work2.handleBulkResult( anyObject(), anyObject() ) ).andReturn( true );
		expect( work3.handleBulkResult( anyObject(), anyObject() ) ).andReturn( false );
		expect( work4.handleBulkResult( anyObject(), anyObject() ) ).andReturn( true );

		replay( errorHandlerMock, work1, work2, work3, work4, work5 );

		processor.executeAsync( Arrays.asList( work1, work2, work3, work4, work5 ) );
		processor.awaitAsyncProcessingCompletion();

		verify( errorHandlerMock, work1, work2, work3, work4, work5 );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( BulkRequestFailedException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 3 ) );
		assertThat( errorContext.getFailingOperations() )
				// work4 shouldn't be marked as failed: bulk execution doesn't stop after a failure
				.containsOnly( luceneWork( 3 ), luceneWork( 5 ) );
	}

	private <T> CompletableFuture<T> completedFuture() {
		return CompletableFuture.completedFuture( null );
	}

	private <T> CompletableFuture<T> failedFuture() {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally( new SearchException() );
		return future.thenApply( (r) -> { throw new SearchException(); } );
	}

	private ElasticsearchWork<?> work(int index) {
		ElasticsearchWork<?> mock = createMock( "work" + index, ElasticsearchWork.class );

		mock.aggregate( anyObject() );
		expectLastCall().andAnswer( () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			aggregator.addNonBulkable( mock );
			return null;
		});

		expect( mock.getLuceneWorks() ).andAnswer( () -> Stream.of( luceneWork( index ) ) ).atLeastOnce();

		return mock;
	}

	private BulkableElasticsearchWork<?> bulkableWork(int index) {
		BulkableElasticsearchWork<?> mock = createMock( "bulkableWork" + index,
				BulkableElasticsearchWork.class );

		mock.aggregate( anyObject() );
		expectLastCall().andAnswer( () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			aggregator.addBulkable( mock );
			return null;
		});

		expect( mock.getBulkableActionMetadata() ).andReturn( null );
		expect( mock.getBulkableActionBody() ).andReturn( null );

		expect( mock.getLuceneWorks() ).andAnswer( () -> Stream.of( luceneWork( index ) ) ).atLeastOnce();

		return mock;
	}

	private LuceneWork luceneWork(int index) {
		LuceneWork work = luceneWorks.get( index );
		if ( work == null ) {
			work = createMock( "luceneWork" + index,
					LuceneWork.class );
			luceneWorks.put( index, work );
		}
		return work;
	}
}
