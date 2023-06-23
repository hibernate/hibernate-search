/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.HttpHost;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types
public class BulkWorkTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ElasticsearchClient clientMock;
	@Mock(strictness = Mock.Strictness.LENIENT)
	private ElasticsearchWorkExecutionContext contextMock;

	@Before
	public void setup() {
		when( contextMock.getClient() ).thenReturn( clientMock );
	}

	@Test
	public void execute_success() {
		BulkableWork<Object> bulkableWork0 = bulkableWork( 0 );
		BulkableWork<Object> bulkableWork1 = bulkableWork( 1 );

		when( bulkableWork0.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 0 ) );
		when( bulkableWork0.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 0 ) );
		when( bulkableWork1.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 1 ) );
		when( bulkableWork1.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 1 ) );
		BulkWork work = new BulkWork.Builder( Arrays.asList( bulkableWork0, bulkableWork1 ) ).build();

		ArgumentCaptor<ElasticsearchRequest> requestCaptor = ArgumentCaptor.forClass( ElasticsearchRequest.class );
		CompletableFuture<ElasticsearchResponse> futureFromClient = new CompletableFuture<>();
		when( clientMock.submit( requestCaptor.capture() ) ).thenReturn( futureFromClient );
		CompletableFuture<BulkResult> returnedFuture = work.execute( contextMock );
		verifyNoOtherClientInteractionsAndReset();
		assertThatFuture( returnedFuture ).isPending();

		assertBulkRequest( requestCaptor.getValue(), 0, 1 );

		JsonObject responseBody = new JsonObject();
		JsonArray items = new JsonArray();
		responseBody.add( "items", items );
		items.add( new JsonObject() );
		items.add( new JsonObject() );
		ElasticsearchResponse response = new ElasticsearchResponse( new HttpHost( "mockHost:9200" ),
				200, "OK", responseBody );
		futureFromClient.complete( response );
		verifyNoOtherClientInteractionsAndReset();

		assertThatFuture( returnedFuture ).isSuccessful();
		BulkResult result = returnedFuture.join();

		Object bulkableResult = new Object();
		when( bulkableWork0.handleBulkResult( same( contextMock ), same( items.get( 0 ).getAsJsonObject() ) ) )
				.thenReturn( bulkableResult );
		assertThat( result.extract( contextMock, bulkableWork0, 0 ) ).isSameAs( bulkableResult );
		verifyNoOtherClientInteractionsAndReset();

		when( bulkableWork1.handleBulkResult( same( contextMock ), same( items.get( 1 ).getAsJsonObject() ) ) )
				.thenReturn( bulkableResult );
		assertThat( result.extract( contextMock, bulkableWork1, 1 ) ).isSameAs( bulkableResult );
		verifyNoOtherClientInteractionsAndReset();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3869")
	public void execute_http500() {
		BulkableWork<Object> bulkableWork0 = bulkableWork( 0 );
		BulkableWork<Object> bulkableWork1 = bulkableWork( 1 );

		when( bulkableWork0.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 0 ) );
		when( bulkableWork0.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 0 ) );
		when( bulkableWork1.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 1 ) );
		when( bulkableWork1.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 1 ) );
		BulkWork work = new BulkWork.Builder( Arrays.asList( bulkableWork0, bulkableWork1 ) ).build();
		verifyNoOtherClientInteractionsAndReset();

		ArgumentCaptor<ElasticsearchRequest> requestCaptor = ArgumentCaptor.forClass( ElasticsearchRequest.class );
		CompletableFuture<ElasticsearchResponse> futureFromClient = new CompletableFuture<>();
		when( clientMock.submit( requestCaptor.capture() ) ).thenReturn( futureFromClient );
		CompletableFuture<BulkResult> returnedFuture = work.execute( contextMock );
		verifyNoOtherClientInteractionsAndReset();
		assertThatFuture( returnedFuture ).isPending();

		assertBulkRequest( requestCaptor.getValue(), 0, 1 );

		JsonObject responseBody = new JsonObject();
		responseBody.addProperty( "someProperty", "someValue" );
		ElasticsearchResponse response = new ElasticsearchResponse( new HttpHost( "mockHost:9200" ),
				500, "SomeStatus", responseBody );
		futureFromClient.complete( response );
		verifyNoOtherClientInteractionsAndReset();

		assertThatFuture( returnedFuture ).isFailed( throwable -> assertThat( throwable )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Elasticsearch response indicates a failure",
						"POST /_bulk",
						"500 'SomeStatus'",
						"someProperty", "someValue"
				) );
	}

	private void assertBulkRequest(ElasticsearchRequest request, int... bulkableIndices) {
		assertThat( request ).isNotNull();
		assertSoftly( softly -> {
			softly.assertThat( request.method() ).isEqualTo( "POST" );
			softly.assertThat( request.path() ).isEqualTo( "/_bulk" );
			List<JsonObject> expectedBodyParts = new ArrayList<>();
			for ( int bulkableIndex : bulkableIndices ) {
				expectedBodyParts.add( bulkableWorkMetadata( bulkableIndex ) );
				expectedBodyParts.add( bulkableWorkBody( bulkableIndex ) );
			}
			softly.assertThat( request.bodyParts() ).containsExactlyElementsOf( expectedBodyParts );
		} );
	}

	private void verifyNoOtherClientInteractionsAndReset() {
		verifyNoMoreInteractions( clientMock );
		reset( clientMock );
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		return mock( BulkableWork.class, "bulkableWork" + index );
	}

	private static JsonObject bulkableWorkMetadata(int index) {
		JsonObject result = new JsonObject();
		result.addProperty( "type", "metadata" );
		result.addProperty( "bulkableWorkIndex", index );
		return result;
	}

	private static JsonObject bulkableWorkBody(int index) {
		JsonObject result = new JsonObject();
		result.addProperty( "type", "body" );
		result.addProperty( "bulkableWorkIndex", index );
		return result;
	}

}
