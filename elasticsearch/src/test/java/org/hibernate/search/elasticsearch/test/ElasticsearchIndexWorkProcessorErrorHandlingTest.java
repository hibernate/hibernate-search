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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

		expect( clientMock.execute( anyObject() ) ).andReturn( new ElasticsearchResponse( 200, "OK", new JsonObject() ) );

		expect( workFactoryMock.bulk( anyObject() ) ).andAnswer( () -> {
			@SuppressWarnings("unchecked")
			List<BulkableElasticsearchWork<?>> bulkableWorks =
					(List<BulkableElasticsearchWork<?>>) EasyMock.getCurrentArguments()[0];
			return new BulkWork.Builder( bulkableWorks );
		} );

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		expect( gsonProviderMock.getGsonPrettyPrinting() ).andReturn( gson );

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
		expect( work.execute( anyObject() ) ).andThrow( new SearchException() );

		replay( errorHandlerMock, work );

		processor.executeSyncSafe( Arrays.asList( work ) );

		verify( errorHandlerMock, work );

		ErrorContext errorContext = capture.getValue();
		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( SearchException.class );
		assertThat( errorContext.getOperationAtFault() ).isSameAs( luceneWork( 1 ) );
		assertThat( errorContext.getFailingOperations() ).containsOnly( luceneWork( 1 ) );
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
