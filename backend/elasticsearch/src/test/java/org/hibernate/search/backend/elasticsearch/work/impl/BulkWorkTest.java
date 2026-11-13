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

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClient;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
// Raw types are the only way to mock parameterized types
@SuppressWarnings({ "unchecked", "rawtypes" })
class BulkWorkTest {

	@Mock
	private ElasticsearchJsonClient clientMock;
	@Mock(strictness = Mock.Strictness.LENIENT)
	private ElasticsearchWorkExecutionContext contextMock;

	@BeforeEach
	void setup() {
		when( contextMock.getClient() ).thenReturn( clientMock );
	}

	@Test
	void execute_success() {
		BulkableWork<Object> bulkableWork0 = bulkableWork( 0 );
		BulkableWork<Object> bulkableWork1 = bulkableWork( 1 );

		when( bulkableWork0.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 0 ) );
		when( bulkableWork0.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 0 ) );
		when( bulkableWork1.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 1 ) );
		when( bulkableWork1.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 1 ) );
		BulkWork work = new BulkWork.Builder( Arrays.asList( bulkableWork0, bulkableWork1 ) ).build();

		ArgumentCaptor<ElasticsearchJsonRequest> requestCaptor = ArgumentCaptor.forClass( ElasticsearchJsonRequest.class );
		CompletableFuture<ElasticsearchJsonResponse> futureFromClient = new CompletableFuture<>();
		when( clientMock.submit( requestCaptor.capture() ) ).thenReturn( futureFromClient );
		CompletableFuture<BulkResult> returnedFuture = work.execute( contextMock );
		verifyNoOtherClientInteractionsAndReset();
		assertThatFuture( returnedFuture ).isPending();

		assertBulkRequest( requestCaptor.getValue(), 0, 1 );
		ObjectNode responseBody = new ObjectNode( JsonNodeFactory.instance );
		ArrayNode items = new ArrayNode( JsonNodeFactory.instance );
		responseBody.set( "items", items );
		items.add( new ObjectNode( JsonNodeFactory.instance ) );
		items.add( new ObjectNode( JsonNodeFactory.instance ) );
		ElasticsearchJsonResponse response = ElasticsearchJsonResponse.withJsonBody( "mockHost:9200",
				200, "OK", responseBody );
		futureFromClient.complete( response );
		verifyNoOtherClientInteractionsAndReset();

		assertThatFuture( returnedFuture ).isSuccessful();
		BulkResult result = returnedFuture.join();

		Object bulkableResult = new Object();
		when( bulkableWork0.handleBulkResult( same( contextMock ), same( ( (ObjectNode) items.get( 0 ) ) ) ) )
				.thenReturn( bulkableResult );
		assertThat( result.extract( contextMock, bulkableWork0, 0 ) ).isSameAs( bulkableResult );
		verifyNoOtherClientInteractionsAndReset();

		when( bulkableWork1.handleBulkResult( same( contextMock ), same( ( (ObjectNode) items.get( 1 ) ) ) ) )
				.thenReturn( bulkableResult );
		assertThat( result.extract( contextMock, bulkableWork1, 1 ) ).isSameAs( bulkableResult );
		verifyNoOtherClientInteractionsAndReset();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3869")
	void execute_http500() {
		BulkableWork<Object> bulkableWork0 = bulkableWork( 0 );
		BulkableWork<Object> bulkableWork1 = bulkableWork( 1 );

		when( bulkableWork0.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 0 ) );
		when( bulkableWork0.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 0 ) );
		when( bulkableWork1.getBulkableActionMetadata() ).thenReturn( bulkableWorkMetadata( 1 ) );
		when( bulkableWork1.getBulkableActionBody() ).thenReturn( bulkableWorkBody( 1 ) );
		BulkWork work = new BulkWork.Builder( Arrays.asList( bulkableWork0, bulkableWork1 ) ).build();
		verifyNoOtherClientInteractionsAndReset();

		ArgumentCaptor<ElasticsearchJsonRequest> requestCaptor = ArgumentCaptor.forClass( ElasticsearchJsonRequest.class );
		CompletableFuture<ElasticsearchJsonResponse> futureFromClient = new CompletableFuture<>();
		when( clientMock.submit( requestCaptor.capture() ) ).thenReturn( futureFromClient );
		CompletableFuture<BulkResult> returnedFuture = work.execute( contextMock );
		verifyNoOtherClientInteractionsAndReset();
		assertThatFuture( returnedFuture ).isPending();

		assertBulkRequest( requestCaptor.getValue(), 0, 1 );
		ObjectNode responseBody = new ObjectNode( JsonNodeFactory.instance );
		responseBody.put( "someProperty", "someValue" );
		ElasticsearchJsonResponse response =
				ElasticsearchJsonResponse.withJsonBody( "mockHost:9200",
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

	private void assertBulkRequest(ElasticsearchJsonRequest request, int... bulkableIndices) {
		assertThat( request ).isNotNull();
		assertSoftly( softly -> {
			softly.assertThat( request.method() ).isEqualTo( "POST" );
			softly.assertThat( request.path() ).isEqualTo( "/_bulk" );
			List<ObjectNode> expectedBodyParts = new ArrayList<>();
			for ( int bulkableIndex : bulkableIndices ) {
				expectedBodyParts.add( bulkableWorkMetadata( bulkableIndex ) );
				expectedBodyParts.add( bulkableWorkBody( bulkableIndex ) );
			}
			softly.assertThat( request.bodyPartsJson() ).containsExactlyElementsOf( expectedBodyParts );
		} );
	}

	private void verifyNoOtherClientInteractionsAndReset() {
		verifyNoMoreInteractions( clientMock );
		reset( clientMock );
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		return mock( BulkableWork.class, "bulkableWork" + index );
	}

	private static ObjectNode bulkableWorkMetadata(int index) {
		ObjectNode result = new ObjectNode( JsonNodeFactory.instance );
		result.put( "type", "metadata" );
		result.put( "bulkableWorkIndex", index );
		return result;
	}

	private static ObjectNode bulkableWorkBody(int index) {
		ObjectNode result = new ObjectNode( JsonNodeFactory.instance );
		result.put( "type", "body" );
		result.put( "bulkableWorkIndex", index );
		return result;
	}

}
