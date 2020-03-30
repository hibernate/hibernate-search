/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;

public class BulkWorkTest extends EasyMockSupport {

	private final ElasticsearchWorkExecutionContext contextMock = createStrictMock( ElasticsearchWorkExecutionContext.class );
	private final ElasticsearchClient clientMock = createStrictMock( ElasticsearchClient.class );

	@Test
	public void execute_success() {
		BulkableWork<Object> bulkableWork0 = bulkableWork( 0 );
		BulkableWork<Object> bulkableWork1 = bulkableWork( 1 );

		resetAll();
		expect( bulkableWork0.getBulkableActionMetadata() ).andReturn( bulkableWorkMetadata( 0 ) );
		expect( bulkableWork0.getBulkableActionBody() ).andReturn( bulkableWorkBody( 0 ) );
		expect( bulkableWork1.getBulkableActionMetadata() ).andReturn( bulkableWorkMetadata( 1 ) );
		expect( bulkableWork1.getBulkableActionBody() ).andReturn( bulkableWorkBody( 1 ) );
		replayAll();
		BulkWork work = new BulkWork.Builder( Arrays.asList( bulkableWork0, bulkableWork1 ) ).build();
		verifyAll();

		Capture<ElasticsearchRequest> requestCapture = Capture.newInstance();
		CompletableFuture<ElasticsearchResponse> futureFromClient = new CompletableFuture<>();
		resetAll();
		expect( contextMock.getClient() ).andStubReturn( clientMock );
		expect( clientMock.submit( capture( requestCapture ) ) ).andReturn( futureFromClient );
		replayAll();
		CompletableFuture<BulkResult> returnedFuture = work.execute( contextMock );
		verifyAll();
		FutureAssert.assertThat( returnedFuture ).isPending();

		assertBulkRequest( requestCapture.getValue(), 0, 1 );

		JsonObject responseBody = new JsonObject();
		JsonArray items = new JsonArray();
		responseBody.add( "items", items );
		items.add( new JsonObject() );
		items.add( new JsonObject() );
		ElasticsearchResponse response = new ElasticsearchResponse( 200, "OK", responseBody );
		resetAll();
		replayAll();
		futureFromClient.complete( response );
		verifyAll();

		FutureAssert.assertThat( returnedFuture ).isSuccessful();
		BulkResult result = returnedFuture.join();

		Object bulkableResult = new Object();
		resetAll();
		expect( bulkableWork0.handleBulkResult( same( contextMock ), same( items.get( 0 ).getAsJsonObject() ) ) )
				.andReturn( bulkableResult );
		replayAll();
		assertThat( result.extract( contextMock, bulkableWork0, 0 ) ).isSameAs( bulkableResult );
		verifyAll();

		resetAll();
		expect( bulkableWork1.handleBulkResult( same( contextMock ), same( items.get( 1 ).getAsJsonObject() ) ) )
				.andReturn( bulkableResult );
		replayAll();
		assertThat( result.extract( contextMock, bulkableWork1, 1 ) ).isSameAs( bulkableResult );
		verifyAll();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3869")
	public void execute_http500() {
		BulkableWork<Object> bulkableWork0 = bulkableWork( 0 );
		BulkableWork<Object> bulkableWork1 = bulkableWork( 1 );

		resetAll();
		expect( bulkableWork0.getBulkableActionMetadata() ).andReturn( bulkableWorkMetadata( 0 ) );
		expect( bulkableWork0.getBulkableActionBody() ).andReturn( bulkableWorkBody( 0 ) );
		expect( bulkableWork1.getBulkableActionMetadata() ).andReturn( bulkableWorkMetadata( 1 ) );
		expect( bulkableWork1.getBulkableActionBody() ).andReturn( bulkableWorkBody( 1 ) );
		replayAll();
		BulkWork work = new BulkWork.Builder( Arrays.asList( bulkableWork0, bulkableWork1 ) ).build();
		verifyAll();

		Capture<ElasticsearchRequest> requestCapture = Capture.newInstance();
		CompletableFuture<ElasticsearchResponse> futureFromClient = new CompletableFuture<>();
		resetAll();
		expect( contextMock.getClient() ).andStubReturn( clientMock );
		expect( clientMock.submit( capture( requestCapture ) ) ).andReturn( futureFromClient );
		replayAll();
		CompletableFuture<BulkResult> returnedFuture = work.execute( contextMock );
		verifyAll();
		FutureAssert.assertThat( returnedFuture ).isPending();

		assertBulkRequest( requestCapture.getValue(), 0, 1 );

		JsonObject responseBody = new JsonObject();
		responseBody.addProperty( "someProperty", "someValue" );
		ElasticsearchResponse response = new ElasticsearchResponse( 500, "SomeStatus", responseBody );
		resetAll();
		replayAll();
		futureFromClient.complete( response );
		verifyAll();

		FutureAssert.assertThat( returnedFuture ).isFailed( throwable -> assertThat( throwable )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Elasticsearch response indicates a failure",
						"POST /_bulk",
						"500 'SomeStatus'",
						"someProperty", "someValue"
				) );
	}

	private void assertBulkRequest(ElasticsearchRequest request, int ... bulkableIndices) {
		assertThat( request ).isNotNull();
		assertSoftly( softly -> {
			softly.assertThat( request.getMethod() ).isEqualTo( "POST" );
			softly.assertThat( request.getPath() ).isEqualTo( "/_bulk" );
			List<JsonObject> expectedBodyParts = new ArrayList<>();
			for ( int bulkableIndex : bulkableIndices ) {
				expectedBodyParts.add( bulkableWorkMetadata( bulkableIndex ) );
				expectedBodyParts.add( bulkableWorkBody( bulkableIndex ) );
			}
			softly.assertThat( request.getBodyParts() ).containsExactlyElementsOf( expectedBodyParts );
		} );
	}

	private <T> BulkableWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableWork.class );
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